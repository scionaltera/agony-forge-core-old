package com.agonyengine.forge.controller;

import com.agonyengine.forge.controller.greeting.GreetingLoader;
import com.agonyengine.forge.controller.interpret.Interpreter;
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
import java.util.List;
import java.util.Map;

import static com.agonyengine.forge.controller.ControllerConstants.AGONY_REMOTE_IP_KEY;

@Controller
public class WebSocketController {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketController.class);

    private List<String> greeting;
    private Interpreter interpreter;

    @Inject
    public WebSocketController(
        @Named("compositeGreetingLoader") GreetingLoader greetingLoader,
        @Named("defaultLoginInterpreter") Interpreter interpreter) {

        greeting = greetingLoader.load();
        this.interpreter = interpreter;
    }

    @Transactional
    @SubscribeMapping("/queue/output")
    public Output onSubscribe(Message<byte[]> message) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.wrap(message);
        Map<String, Object> attributes = headerAccessor.getSessionAttributes();

        LOGGER.info("New connection from {}", attributes == null ? "(unknown)" : attributes.get(AGONY_REMOTE_IP_KEY));

        return new Output(greeting).append(interpreter.prompt(attributes));
    }

    @Transactional
    @MessageMapping("/input")
    @SendToUser(value = "/queue/output", broadcast = false)
    public Output onInput(Input input, Message<byte[]> message) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.wrap(message);
        Map<String, Object> attributes = headerAccessor.getSessionAttributes();

        return interpreter.interpret(input, attributes);
    }
}
