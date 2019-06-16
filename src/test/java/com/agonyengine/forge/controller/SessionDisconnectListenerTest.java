package com.agonyengine.forge.controller;

import org.junit.Before;
import org.junit.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.agonyengine.forge.controller.ControllerConstants.AGONY_REMOTE_IP_KEY;
import static org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor.HTTP_SESSION_ID_ATTR_NAME;

public class SessionDisconnectListenerTest {
    private Message<byte[]> message = buildMockMessage();
    private SessionDisconnectEvent event;

    private SessionDisconnectListener listener;

    @Before
    public void setUp() {
        event = new SessionDisconnectEvent("source", message, "ffff", CloseStatus.NORMAL);

        listener = new SessionDisconnectListener();
    }

    @Test
    public void testOnApplicationEvent() {
        listener.onApplicationEvent(event);
    }

    private Message<byte[]> buildMockMessage() {
        UUID springSessionId = UUID.randomUUID();
        UUID stompSessionId = UUID.randomUUID();
        Map<String, Object> sessionAttributes = new HashMap<>();
        Map<String, Object> headers = new HashMap<>();

        sessionAttributes.put(HTTP_SESSION_ID_ATTR_NAME, springSessionId.toString());
        sessionAttributes.put(AGONY_REMOTE_IP_KEY, "12.34.56.78");

        headers.put(SimpMessageHeaderAccessor.SESSION_ID_HEADER, stompSessionId.toString());
        headers.put(SimpMessageHeaderAccessor.SESSION_ATTRIBUTES, sessionAttributes);

        return new GenericMessage<>(new byte[0], headers);
    }
}
