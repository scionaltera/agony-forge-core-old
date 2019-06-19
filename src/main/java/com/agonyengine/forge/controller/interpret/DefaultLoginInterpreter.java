package com.agonyengine.forge.controller.interpret;

import com.agonyengine.forge.controller.Input;
import com.agonyengine.forge.controller.Output;
import com.agonyengine.forge.model.Connection;
import com.agonyengine.forge.model.Creature;
import com.agonyengine.forge.repository.CreatureRepository;
import com.agonyengine.forge.service.CommService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static com.agonyengine.forge.controller.ControllerConstants.*;
import static com.agonyengine.forge.controller.ControllerConstants.AGONY_STOMP_SESSION_KEY;
import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;
import static org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor.HTTP_SESSION_ID_ATTR_NAME;

@Component
public class DefaultLoginInterpreter implements Interpreter {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultLoginInterpreter.class);

    static final String CURRENT_STATE_KEY = "AGONY.CURRENT.STATE";
    static final String TEMP_NAME_KEY = "AGONY.NAME";

    enum InterpreterState {
        ASK_NEW,
        LOGIN_ASK_NAME,
        LOGIN_ASK_PASSWORD,
        CREATE_CHOOSE_NAME,
        CREATE_CONFIRM_NAME,
        CREATE_CHOOSE_PASSWORD,
        CREATE_CONFIRM_PASSWORD,
        LOGGED_IN
    }

    private UserDetailsManager userDetailsManager;
    private AuthenticationManager authenticationManager;
    private PasswordEncoder passwordEncoder;
    private SessionRepository sessionRepository;
    private CreatureRepository creatureRepository;
    private CommService commService;

    @Inject
    public DefaultLoginInterpreter(@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") UserDetailsManager userDetailsManager,
                                   AuthenticationManager authenticationManager,
                                   SessionRepository sessionRepository,
                                   CreatureRepository creatureRepository,
                                   CommService commService) {

        this.userDetailsManager = userDetailsManager;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        this.sessionRepository = sessionRepository;
        this.creatureRepository = creatureRepository;
        this.commService = commService;
    }

    @Transactional
    @Override
    public Output interpret(Input input, Map<String, Object> attributes) {
        Output output = new Output();
        InterpreterState currentState = (InterpreterState)attributes.getOrDefault(CURRENT_STATE_KEY, InterpreterState.ASK_NEW);
        String name = (String)attributes.get(TEMP_NAME_KEY);
        String remoteAddress = (String)attributes.get(AGONY_REMOTE_IP_KEY);

        switch (currentState) {
            case ASK_NEW:
                if (input.toString().equalsIgnoreCase("Y")) {
                    attributes.put(CURRENT_STATE_KEY, InterpreterState.CREATE_CHOOSE_NAME);
                } else {
                    attributes.put(CURRENT_STATE_KEY, InterpreterState.LOGIN_ASK_NAME);
                }
                break;
            case LOGIN_ASK_NAME:
                try {
                    name = validateName(input.toString());

                    attributes.put(TEMP_NAME_KEY, name);
                    attributes.put(CURRENT_STATE_KEY, InterpreterState.LOGIN_ASK_PASSWORD);
                } catch (InvalidInputException e) {
                    output.append("[red]" + e.getMessage());
                }
                break;
            case LOGIN_ASK_PASSWORD:
                try {
                    logUserIn(name, validatePassword(input.toString()), attributes);

                    Creature creature = buildPlayerCreature(name, attributes);
                    Creature savedCreature = creatureRepository.save(creature);

                    attributes.put(AGONY_CREATURE_KEY, savedCreature.getId());
                    attributes.put(CURRENT_STATE_KEY, InterpreterState.LOGGED_IN);

                    output.append("[yellow]Welcome, " + name + "!");

                    LOGGER.info("Successful login for {} from {}", name, attributes.get(AGONY_REMOTE_IP_KEY));
                } catch (InvalidInputException e) {
                    output.append("[red]" + e.getMessage());
                } catch (BadCredentialsException e) {
                    output.append("[red]Sorry! Please try again!");
                    LOGGER.warn("Bad password attempt for {} from {}", name, remoteAddress);
                    attributes.put(CURRENT_STATE_KEY, InterpreterState.ASK_NEW);
                }
                break;
            case CREATE_CHOOSE_NAME:
                try {
                    name = validateName(input.toString());

                    if (userDetailsManager.userExists(name)) {
                        output.append("[red]That name is already in use. Please try another!");
                        attributes.put(CURRENT_STATE_KEY, InterpreterState.CREATE_CHOOSE_NAME);
                    } else {
                        attributes.put(TEMP_NAME_KEY, name);
                        attributes.put(CURRENT_STATE_KEY, InterpreterState.CREATE_CONFIRM_NAME);
                    }
                } catch (InvalidInputException e) {
                    output.append("[red]" + e.getMessage());
                }
                break;
            case CREATE_CONFIRM_NAME:
                if (input.toString().equalsIgnoreCase("Y")) {
                    attributes.put(CURRENT_STATE_KEY, InterpreterState.CREATE_CHOOSE_PASSWORD);
                } else {
                    attributes.put(CURRENT_STATE_KEY, InterpreterState.CREATE_CHOOSE_NAME);
                }
                break;
            case CREATE_CHOOSE_PASSWORD:
                try {
                    User user = new User(
                        name,
                        passwordEncoder.encode(validatePassword(input.toString())),
                        true,
                        true,
                        true,
                        true,
                        Collections.singletonList(new SimpleGrantedAuthority("PLAYER")));

                    userDetailsManager.createUser(user);

                    logUserIn(name, validatePassword(input.toString()), attributes);
                    attributes.put(CURRENT_STATE_KEY, InterpreterState.CREATE_CONFIRM_PASSWORD);
                } catch (InvalidInputException e) {
                    output.append("[red]" + e.getMessage());
                } catch (BadCredentialsException e) {
                    output.append("[red]Oops! Something bad happened. The error has been logged.");
                    LOGGER.error("Unable to log in newly created player!", e);
                }

                break;
            case CREATE_CONFIRM_PASSWORD:
                try {
                    logUserIn(name, validatePassword(input.toString()), attributes);

                    Creature creature = buildPlayerCreature(name, attributes);
                    Creature savedCreature = creatureRepository.save(creature);

                    attributes.put(AGONY_CREATURE_KEY, savedCreature.getId());
                    attributes.put(CURRENT_STATE_KEY, InterpreterState.LOGGED_IN);

                    output.append("[yellow]Welcome, " + name + "!");

                    LOGGER.info("New player {} from {}", name, attributes.get(AGONY_REMOTE_IP_KEY));
                } catch (InvalidInputException e) {
                    output.append("[red]" + e.getMessage());
                } catch (BadCredentialsException e) {
                    output.append("[red]Passwords do not match. Please try again!");
                    userDetailsManager.deleteUser(name);
                    attributes.put(CURRENT_STATE_KEY, InterpreterState.CREATE_CHOOSE_PASSWORD);
                }
                break;
            case LOGGED_IN:
                Creature creature = creatureRepository.findById((UUID)attributes.get(AGONY_CREATURE_KEY)).orElse(null);

                output.append("[green]You gossip '" + input.toString() + "[green]'", "");
                commService.echoToWorld(
                    new Output("[green]" + name + " gossips '" + input.toString() + "[green]'", "")
                        .append(prompt(attributes)), // TODO this needs be the target's prompt, not ours!
                    creature);
                break;
            default:
                output.append("[red]Oops! Something went wrong. The error has been logged.", "");
                LOGGER.error("Reached default state in interpret()!");
        }

        return output.append(prompt(attributes));
    }

    @Transactional
    @Override
    public Output prompt(Map<String, Object> attributes) {
        InterpreterState currentState = (InterpreterState)attributes.getOrDefault(CURRENT_STATE_KEY, InterpreterState.ASK_NEW);
        String name = (String)attributes.get(TEMP_NAME_KEY);

        switch (currentState) {
            case ASK_NEW:
                return new Output("[default]Create a new character? [y/N]: ");
            case LOGIN_ASK_NAME:
                return new Output("[default]Name: ");
            case LOGIN_ASK_PASSWORD:
                return new Output("[default]Password: ").setSecret(true);
            case CREATE_CHOOSE_NAME:
                return new Output("[default]Please choose a name: ");
            case CREATE_CONFIRM_NAME:
                return new Output("[default]Are you sure '" + name + "' is the name you want? [y/N]: ");
            case CREATE_CHOOSE_PASSWORD:
                return new Output("[default]Please choose a password: ").setSecret(true);
            case CREATE_CONFIRM_PASSWORD:
                return new Output("[default]Please confirm your password: ").setSecret(true);
            case LOGGED_IN:
                return new Output(String.format("[default]%s> ", name));
            default:
                LOGGER.error("Reached default state in prompt()!");
                return new Output("[red]Oops! Something went wrong. The error has been logged.");
        }
    }

    private String validateName(String in) throws InvalidInputException {
        if (in.matches(".*\\s.*")) {
            throw new InvalidInputException("Names may not contain whitespace.");
        }

        if (in.matches(".*[^a-zA-Z].*")) {
            throw new InvalidInputException("Names may only contain letters.");
        }

        if (in.length() < 3) {
            throw new InvalidInputException("Names must be at least 3 letters long.");
        }

        if (in.length() > 30) {
            throw new InvalidInputException("Names may not be longer than 30 letters.");
        }

        if (in.matches("^[^A-Z].*$")) {
            throw new InvalidInputException("Names must begin with an upper case letter.");
        }

        if (in.matches("^[A-Z].*[A-Z].*$")) {
            throw new InvalidInputException("Names must not contain upper case letters other than the first.");
        }

        return in;
    }

    private String validatePassword(String in) throws InvalidInputException {
        if (in.length() < 8) {
            throw new InvalidInputException("Passwords must be at least 8 characters.");
        }

        return in;
    }

    @SuppressWarnings("unchecked")
    private void logUserIn(String name, String password, Map<String, Object> attributes) {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(name, password);
        Authentication authentication = authenticationManager.authenticate(token);
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Session session = sessionRepository.findById((String)attributes.get(HTTP_SESSION_ID_ATTR_NAME));

        securityContext.setAuthentication(authentication);
        session.setAttribute(SPRING_SECURITY_CONTEXT_KEY, securityContext);

        sessionRepository.save(session);
    }

    private Creature buildPlayerCreature(String name, Map<String, Object> attributes) {
        User user = (User)userDetailsManager.loadUserByUsername(name);
        Creature creature = new Creature();
        Connection connection = new Connection();

        connection.setSessionUsername((String)attributes.get(AGONY_STOMP_PRINCIPAL_KEY));
        connection.setSessionId((String)attributes.get(AGONY_STOMP_SESSION_KEY));
        connection.setRemoteAddress((String)attributes.get(AGONY_REMOTE_IP_KEY));

        creature.setName(user.getUsername());
        creature.setConnection(connection);

        return creature;
    }
}
