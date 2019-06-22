package com.agonyengine.forge.controller;

import com.agonyengine.forge.controller.greeting.ClasspathGreetingLoader;
import com.agonyengine.forge.controller.interpret.Interpreter;
import com.agonyengine.forge.model.Connection;
import com.agonyengine.forge.repository.ConnectionRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.agonyengine.forge.controller.ControllerConstants.AGONY_CONNECTION_ID_KEY;
import static com.agonyengine.forge.controller.ControllerConstants.AGONY_REMOTE_IP_KEY;
import static com.agonyengine.forge.model.ConnectionState.ASK_NEW;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor.HTTP_SESSION_ID_ATTR_NAME;

public class WebSocketControllerTest {
    @Mock
    private ConnectionRepository connectionRepository;

    @Mock
    private Interpreter interpreter;

    @Captor
    private ArgumentCaptor<Connection> connectionCaptor;

    private Principal principal = new UsernamePasswordAuthenticationToken("user", "pass");

    private WebSocketController controller;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        ClasspathGreetingLoader loader = new ClasspathGreetingLoader();
        Connection connection = new Connection();

        when(connectionRepository.save(any(Connection.class))).thenAnswer(invocation -> {
            Connection c = invocation.getArgument(0);

            c.setId(UUID.randomUUID());

            return c;
        });
        when(connectionRepository.findById(any(UUID.class))).thenReturn(Optional.of(connection));
        when(interpreter.prompt(any(Connection.class))).thenReturn(new Output("[default]> "));
        when(interpreter.interpret(any(Input.class), any(Connection.class))).thenAnswer(invocation -> {
            Input input = invocation.getArgument(0);

            return new Output("[cyan]" + input.toString(), "[default]> ");
        });

        controller = new WebSocketController(loader, connectionRepository, interpreter);
    }

    @Test
    public void testOnSubscribe() {
        Message<byte[]> message = buildMockMessage(true, false);

        Output o1 = new Output(
            "[yellow]Hello&nbsp;world!",
            "[yellow]Hello world!",
            "[default]> ");
        Output o2 = controller.onSubscribe(principal, message);

        verify(connectionRepository).save(connectionCaptor.capture());

        assertEquals(o1, o2);

        Connection connection = connectionCaptor.getValue();

        assertEquals(principal.getName(), connection.getSessionUsername());
        assertNotNull(UUID.fromString(connection.getSessionId()));
        assertNotNull(UUID.fromString(connection.getHttpSessionId()));
        assertEquals("1.2.3.4", connection.getRemoteAddress());
        assertEquals(ASK_NEW, connection.getState());
    }

    @Test
    public void testOnSubscribeNoAttributes() {
        Message<byte[]> message = buildMockMessage(false, false);

        assertEquals(new Output("[red]Something went wrong! The error has been logged."), controller.onSubscribe(principal, message));
    }

    @Test
    public void testOnInput() {
        Message<byte[]> message = buildMockMessage(true, true);
        Input input = new Input();

        input.setInput("Testing");

        assertEquals(new Output("[cyan]" + input, "[default]> "), controller.onInput(input, message));
    }

    @Test
    public void testOnInputNoConnection() {
        Message<byte[]> message = buildMockMessage(true, true);
        Input input = new Input();

        input.setInput("Testing");

        when(connectionRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        try {
            controller.onInput(input, message);
            fail("Required exception was not thrown.");
        } catch (NullPointerException e) {
            assertTrue(e.getMessage().startsWith("Unable to fetch Connection by ID: "));
        }
    }

    @Test
    public void testOnInputNoAttributes() {
        Message<byte[]> message = buildMockMessage(false, true);
        Input input = new Input();

        input.setInput("Testing");

        assertEquals(new Output("[red]Something went wrong! The error has been logged."), controller.onInput(input, message));
    }

    private Message<byte[]> buildMockMessage(boolean includeAttributes, boolean includeConnectionId) {
        UUID springSessionId = UUID.randomUUID();
        UUID stompSessionId = UUID.randomUUID();
        Map<String, Object> sessionAttributes = new HashMap<>();
        Map<String, Object> headers = new HashMap<>();

        sessionAttributes.put(HTTP_SESSION_ID_ATTR_NAME, springSessionId.toString());
        sessionAttributes.put(AGONY_REMOTE_IP_KEY, "1.2.3.4");

        if (includeConnectionId) {
            sessionAttributes.put(AGONY_CONNECTION_ID_KEY, UUID.randomUUID());
        }

        headers.put(SimpMessageHeaderAccessor.SESSION_ID_HEADER, stompSessionId.toString());

        if (includeAttributes) {
            headers.put(SimpMessageHeaderAccessor.SESSION_ATTRIBUTES, sessionAttributes);
        }

        return new GenericMessage<>(new byte[0], headers);
    }
}
