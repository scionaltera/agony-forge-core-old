package com.agonyengine.forge.controller.interpret;

import com.agonyengine.forge.controller.Input;
import com.agonyengine.forge.controller.Output;
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
import java.util.Collections;
import java.util.Map;

import static com.agonyengine.forge.controller.ControllerConstants.AGONY_REMOTE_IP_KEY;
import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;
import static org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor.HTTP_SESSION_ID_ATTR_NAME;

@Component
public class SimpleLoginInterpreter implements Interpreter {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleLoginInterpreter.class);
    private static final String CURRENT_STATE_KEY = "AGONY.CURRENT.STATE";
    private static final String TEMP_NAME_KEY = "AGONY.NAME";

    private enum InterpreterState {
        ASK_NAME,
        ASK_PASSWORD,
        CHOOSE_PASSWORD,
        LOGGED_IN
    }

    private UserDetailsManager userDetailsManager;
    private AuthenticationManager authenticationManager;
    private PasswordEncoder passwordEncoder;
    private SessionRepository sessionRepository;

    @Inject
    public SimpleLoginInterpreter(@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") UserDetailsManager userDetailsManager,
                                  AuthenticationManager authenticationManager,
                                  SessionRepository sessionRepository) {

        this.userDetailsManager = userDetailsManager;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        this.sessionRepository = sessionRepository;
    }

    @Override
    public Output interpret(Input input, Map<String, Object> attributes) {
        Output output = new Output();
        InterpreterState currentState = (InterpreterState)attributes.getOrDefault(CURRENT_STATE_KEY, InterpreterState.ASK_NAME);
        String name = (String)attributes.get(TEMP_NAME_KEY);
        String remoteAddress = (String)attributes.get(AGONY_REMOTE_IP_KEY);

        switch (currentState) {
            case ASK_NAME:
                name = input.toString();
                attributes.put(TEMP_NAME_KEY, name);

                if (userDetailsManager.userExists(name)) {
                    attributes.put(CURRENT_STATE_KEY, InterpreterState.ASK_PASSWORD);
                } else {
                    attributes.put(CURRENT_STATE_KEY, InterpreterState.CHOOSE_PASSWORD);
                }
                break;
            case ASK_PASSWORD:
                try {
                    logUserIn(name, input.toString(), attributes);
                } catch (BadCredentialsException e) {
                    output.append("[red]Nope!");
                    LOGGER.warn("Bad password attempt for {} from {}", name, remoteAddress);
                }

                break;
            case CHOOSE_PASSWORD:
                User user = new User(
                    name,
                    passwordEncoder.encode(input.toString()),
                    true,
                    true,
                    true,
                    true,
                    Collections.singletonList(new SimpleGrantedAuthority("PLAYER")));

                userDetailsManager.createUser(user);

                LOGGER.info("New user {} from {}", name, remoteAddress);

                try {
                    logUserIn(name, input.toString(), attributes);
                } catch (BadCredentialsException e) {
                    output.append("[red]Weird...");
                }

                output.append("[default]Welcome, " + name + "!", "");
                attributes.put(CURRENT_STATE_KEY, InterpreterState.LOGGED_IN);
                break;
            case LOGGED_IN:
                output.append("[cyan]" + input.toString(), "");
                break;
            default:
                output.append("[red]This should never happen.", "");
                break;
        }

        return output.append(prompt(attributes));
    }

    @Override
    public Output prompt(Map<String, Object> attributes) {
        InterpreterState currentState = (InterpreterState)attributes.getOrDefault(CURRENT_STATE_KEY, InterpreterState.ASK_NAME);
        String name = (String)attributes.get(TEMP_NAME_KEY);

        switch (currentState) {
            case ASK_NAME:
                return new Output("[default]What is your name?");
            case ASK_PASSWORD:
                return new Output("[default]Password: ");
            case CHOOSE_PASSWORD:
                return new Output("[default]Please choose a password: ");
            case LOGGED_IN:
                return new Output(String.format("[default]%s> ", name));
            default:
                return new Output("[red]This prompt should never happen.");
        }
    }

    private void logUserIn(String name, String password, Map<String, Object> attributes) {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(name, password);
        Authentication authentication = authenticationManager.authenticate(token);
        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(authentication);

        Session session = sessionRepository.findById((String)attributes.get(HTTP_SESSION_ID_ATTR_NAME));

        session.setAttribute(SPRING_SECURITY_CONTEXT_KEY, securityContext);
        //noinspection unchecked
        sessionRepository.save(session);

        LOGGER.info("Successful login for {} from {}", name, attributes.get(AGONY_REMOTE_IP_KEY));

        attributes.put(CURRENT_STATE_KEY, InterpreterState.LOGGED_IN);
    }
}
