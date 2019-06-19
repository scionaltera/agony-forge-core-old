package com.agonyengine.forge.controller;

import com.agonyengine.forge.controller.greeting.ClasspathGreetingLoader;
import com.agonyengine.forge.controller.interpret.EchoInterpreter;
import com.agonyengine.forge.controller.interpret.Interpreter;
import org.junit.Before;
import org.junit.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.GenericMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor.HTTP_SESSION_ID_ATTR_NAME;

public class WebSocketControllerTest {
    private Message<byte[]> message = buildMockMessage();
    private Interpreter interpreter = new EchoInterpreter();

    private WebSocketController controller;

    @Before
    public void setUp() {
        ClasspathGreetingLoader loader = new ClasspathGreetingLoader();

        controller = new WebSocketController(loader, interpreter);
    }

    @Test
    public void testOnSubscribe() {
        Output o1 = new Output(
            "[yellow]Hello&nbsp;world!",
            "[yellow]Hello world!",
            "[default]> ");
        Output o2 = controller.onSubscribe(message);

        assertEquals(o1, o2);
    }

    @Test
    public void testOnInput() {
        Input input = new Input();

        input.setInput("Testing");

        assertEquals(new Output("[cyan]" + input, "[default]> "), controller.onInput(input, message));
    }

    private Message<byte[]> buildMockMessage() {
        UUID springSessionId = UUID.randomUUID();
        UUID stompSessionId = UUID.randomUUID();
        Map<String, Object> sessionAttributes = new HashMap<>();
        Map<String, Object> headers = new HashMap<>();

        sessionAttributes.put(HTTP_SESSION_ID_ATTR_NAME, springSessionId.toString());

        headers.put(SimpMessageHeaderAccessor.SESSION_ID_HEADER, stompSessionId.toString());
        headers.put(SimpMessageHeaderAccessor.SESSION_ATTRIBUTES, sessionAttributes);

        return new GenericMessage<>(new byte[0], headers);
    }
}