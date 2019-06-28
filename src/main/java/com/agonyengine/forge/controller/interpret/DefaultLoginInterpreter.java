package com.agonyengine.forge.controller.interpret;

import com.agonyengine.forge.config.LoginConfiguration;
import com.agonyengine.forge.controller.Input;
import com.agonyengine.forge.controller.Output;
import com.agonyengine.forge.model.Connection;
import com.agonyengine.forge.model.ConnectionState;
import com.agonyengine.forge.model.Creature;
import com.agonyengine.forge.repository.ConnectionRepository;
import com.agonyengine.forge.repository.CreatureRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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

import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

@Component
public class DefaultLoginInterpreter extends BaseInterpreter {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultLoginInterpreter.class);

    private LoginConfiguration loginConfiguration;
    private UserDetailsManager userDetailsManager;
    private AuthenticationManager authenticationManager;
    private PasswordEncoder passwordEncoder;
    private SessionRepository sessionRepository;
    private ConnectionRepository connectionRepository;
    private CreatureRepository creatureRepository;

    @Inject
    public DefaultLoginInterpreter(
        LoginConfiguration loginConfiguration,
        @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") UserDetailsManager userDetailsManager,
        AuthenticationManager authenticationManager,
        SessionRepository sessionRepository,
        ConnectionRepository connectionRepository,
        CreatureRepository creatureRepository,
        SimpMessagingTemplate simpMessagingTemplate) {

        super(creatureRepository, simpMessagingTemplate);

        this.loginConfiguration = loginConfiguration;
        this.userDetailsManager = userDetailsManager;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        this.sessionRepository = sessionRepository;
        this.connectionRepository = connectionRepository;
        this.creatureRepository = creatureRepository;
    }

    @Transactional
    @Override
    public Output interpret(Input input, Connection connection) {
        Output output = new Output();

        switch (connection.getState()) {
            case ASK_NEW:
                if (input.toString().equalsIgnoreCase("Y")) {
                    connection.setState(ConnectionState.CREATE_CHOOSE_NAME);
                } else {
                    connection.setState(ConnectionState.LOGIN_ASK_NAME);
                }
                break;
            case LOGIN_ASK_NAME:
                try {
                    connection.setName(validateName(input.toString()));
                    connection.setState(ConnectionState.LOGIN_ASK_PASSWORD);
                } catch (InvalidInputException e) {
                    output.append("[red]" + e.getMessage());
                }
                break;
            case LOGIN_ASK_PASSWORD:
                try {
                    logUserIn(connection.getName(), input.toString(), connection);
                    buildCreature(connection.getName(), connection);

                    output.append("[yellow]Welcome back, " + connection.getName() + "!");

                    LOGGER.info("Successful login for {} from {}", connection.getName(), connection.getRemoteAddress());
                } catch (BadCredentialsException e) {
                    output.append("[red]Sorry! Please try again!");
                    LOGGER.warn("Bad password attempt for {} from {}", connection.getName(), connection.getRemoteAddress());
                    connection.setState(ConnectionState.ASK_NEW);
                }
                break;
            case CREATE_CHOOSE_NAME:
                try {
                    connection.setName(validateName(input.toString()));

                    if (userDetailsManager.userExists(connection.getName())) {
                        output.append("[red]That name is already in use. Please try another!");
                        connection.setName(null);
                    } else {
                        connection.setState(ConnectionState.CREATE_CONFIRM_NAME);
                    }
                } catch (InvalidInputException e) {
                    output.append("[red]" + e.getMessage());
                }
                break;
            case CREATE_CONFIRM_NAME:
                if (input.toString().equalsIgnoreCase("Y")) {
                    connection.setState(ConnectionState.CREATE_CHOOSE_PASSWORD);
                } else {
                    connection.setState(ConnectionState.CREATE_CHOOSE_NAME);
                }
                break;
            case CREATE_CHOOSE_PASSWORD:
                try {
                    User user = new User(
                        connection.getName(),
                        passwordEncoder.encode(validatePassword(input.toString())),
                        true,
                        true,
                        true,
                        true,
                        Collections.singletonList(new SimpleGrantedAuthority("PLAYER")));

                    userDetailsManager.createUser(user);

                    logUserIn(connection.getName(), validatePassword(input.toString()), connection);
                    connection.setState(ConnectionState.CREATE_CONFIRM_PASSWORD);
                } catch (InvalidInputException e) {
                    output.append("[red]" + e.getMessage());
                } catch (BadCredentialsException e) {
                    output.append("[red]Oops! Something bad happened. The error has been logged.");
                    LOGGER.error("Unable to log in newly created player!", e);
                }

                break;
            case CREATE_CONFIRM_PASSWORD:
                try {
                    logUserIn(connection.getName(), validatePassword(input.toString()), connection);
                    buildCreature(connection.getName(), connection);

                    output.append("[yellow]Welcome, " + connection.getName() + "!");

                    LOGGER.info("New player {} from {}", connection.getName(), connection.getRemoteAddress());
                } catch (InvalidInputException e) {
                    output.append("[red]" + e.getMessage());
                } catch (BadCredentialsException e) {
                    output.append("[red]Passwords do not match. Please try again!");
                    userDetailsManager.deleteUser(connection.getName());
                    connection.setState(ConnectionState.CREATE_CHOOSE_PASSWORD);
                }
                break;
            case IN_GAME:
                Creature creature = creatureRepository
                    .findByConnection(connection)
                    .orElseThrow(() -> new NullPointerException("Unable to find Creature for Connection " + connection.getId()));

                output.append("[green]You gossip '" + input.toString() + "[green]'");
                echoToWorld(new Output("[green]" + connection.getName() + " gossips '" + input.toString() + "[green]'"), creature);
                break;
            default:
                output.append("[red]Oops! Something went wrong. The error has been logged.");
                LOGGER.error("Reached default state in interpret()!");
        }

        return output.append(prompt(connectionRepository.save(connection)));
    }

    @Transactional
    @Override
    public Output prompt(Connection connection) {

        switch (connection.getState()) {
            case ASK_NEW:
                return new Output(loginConfiguration.getPrompt("askNew", connection));
            case LOGIN_ASK_NAME:
                return new Output(loginConfiguration.getPrompt("loginAskName", connection));
            case LOGIN_ASK_PASSWORD:
                return new Output(loginConfiguration.getPrompt("loginAskPassword", connection)).setSecret(true);
            case CREATE_CHOOSE_NAME:
                return new Output(loginConfiguration.getPrompt("createChooseName", connection));
            case CREATE_CONFIRM_NAME:
                return new Output(loginConfiguration.getPrompt("createConfirmName", connection));
            case CREATE_CHOOSE_PASSWORD:
                return new Output(loginConfiguration.getPrompt("createChoosePassword", connection)).setSecret(true);
            case CREATE_CONFIRM_PASSWORD:
                return new Output(loginConfiguration.getPrompt("createConfirmPassword", connection)).setSecret(true);
            case IN_GAME:
                return new Output("", loginConfiguration.getPrompt("inGame", connection));
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
    private void logUserIn(String name, String password, Connection connection) {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(name, password);
        Authentication authentication = authenticationManager.authenticate(token);
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Session session = sessionRepository.findById(connection.getHttpSessionId());

        securityContext.setAuthentication(authentication);
        session.setAttribute(SPRING_SECURITY_CONTEXT_KEY, securityContext);

        sessionRepository.save(session);
    }

    private void buildCreature(String name, Connection connection) {
        Creature creature = new Creature();

        creature.setName(name);
        creature.setConnection(connection);

        creatureRepository.save(creature);

        connection.setState(ConnectionState.IN_GAME);
    }
}
