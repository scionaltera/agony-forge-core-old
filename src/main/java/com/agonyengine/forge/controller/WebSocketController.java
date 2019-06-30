package com.agonyengine.forge.controller;

import com.agonyengine.forge.controller.greeting.GreetingLoader;
import com.agonyengine.forge.controller.interpret.Interpreter;
import com.agonyengine.forge.model.Connection;
import com.agonyengine.forge.model.PrimaryConnectionState;
import com.agonyengine.forge.repository.ConnectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.agonyengine.forge.controller.ControllerConstants.*;
import static org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor.HTTP_SESSION_ID_ATTR_NAME;

@Controller
public class WebSocketController {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketController.class);

    private List<String> greeting;
    private ConnectionRepository connectionRepository;
    private Interpreter interpreter;

    @Inject
    public WebSocketController(
        @Named("compositeGreetingLoader") GreetingLoader greetingLoader,
        ConnectionRepository connectionRepository,
        Interpreter interpreter) {

        greeting = greetingLoader.load();
        this.connectionRepository = connectionRepository;
        this.interpreter = interpreter;
    }

    @Transactional
    @SubscribeMapping("/queue/output")
    public Output onSubscribe(Principal principal, Message <byte[]> message) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.wrap(message);
        Map<String, Object> attributes = headerAccessor.getSessionAttributes();
        Connection connection = new Connection();

        if (attributes != null) {
            connection.setSessionUsername(principal.getName());
            connection.setSessionId(headerAccessor.getSessionId());
            connection.setHttpSessionId((String) attributes.get(HTTP_SESSION_ID_ATTR_NAME));
            connection.setRemoteAddress((String) attributes.get(AGONY_REMOTE_IP_KEY));
            connection.setPrimaryState(PrimaryConnectionState.LOGIN);

            Connection saved = connectionRepository.save(connection);

            attributes.put(AGONY_CONNECTION_ID_KEY, saved.getId());

            LOGGER.info("New connection from {}", attributes.get(AGONY_REMOTE_IP_KEY));

            return new Output(greeting).append(interpreter.prompt(connection));
        }

        LOGGER.error("Unable to get session attributes!");
        return new Output("[red]Something went wrong! The error has been logged.");
    }

    @Transactional
    @MessageMapping("/input")
    @SendToUser(value = "/queue/output", broadcast = false)
    public Output onInput(Input input, Message<byte[]> message) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.wrap(message);
        Map<String, Object> attributes = headerAccessor.getSessionAttributes();

        if (attributes != null) {
            UUID connectionId = (UUID) attributes.get(AGONY_CONNECTION_ID_KEY);
            Connection connection = connectionRepository
                .findById(connectionId)
                .orElseThrow(() -> new NullPointerException("Unable to fetch Connection by ID: " + connectionId));

            return interpreter.interpret(input, connection);
        }

        LOGGER.error("Unable to get session attributes!");
        return new Output("[red]Something went wrong! The error has been logged.");
    }
}
