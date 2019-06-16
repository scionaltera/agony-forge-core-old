package com.agonyengine.forge.controller;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.agonyengine.forge.controller.ControllerConstants.AGONY_REMOTE_IP_KEY;
import static com.agonyengine.forge.controller.RemoteIpHandshakeInterceptor.X_FORWARDED_FOR_HEADER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class RemoteIpHandshakeInterceptorTest {
    @Mock
    private ServerHttpRequest request;

    @Mock
    private ServerHttpResponse response;

    @Mock
    private WebSocketHandler wsHandler;

    @Mock
    private HttpHeaders httpHeaders;

    private RemoteIpHandshakeInterceptor interceptor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        interceptor = new RemoteIpHandshakeInterceptor();
    }

    @Test
    public void testBeforeHandshakeNoForwarding() {
        Map<String, Object> attributes = new HashMap<>();

        when(request.getHeaders()).thenReturn(httpHeaders);
        when(request.getRemoteAddress()).thenReturn(new InetSocketAddress("8.8.8.8", 32000));

        assertTrue(interceptor.beforeHandshake(request, response, wsHandler, attributes));

        assertEquals("8.8.8.8", attributes.get(AGONY_REMOTE_IP_KEY));
    }

    @Test
    public void testBeforeHandshakeForwarded() {
        Map<String, Object> attributes = new HashMap<>();

        when(request.getHeaders()).thenReturn(httpHeaders);
        when(request.getRemoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 32000));
        when(httpHeaders.get(X_FORWARDED_FOR_HEADER)).thenReturn(Collections.singletonList("192.168.44.11,8.8.8.8"));

        assertTrue(interceptor.beforeHandshake(request, response, wsHandler, attributes));

        assertEquals("8.8.8.8", attributes.get(AGONY_REMOTE_IP_KEY));
    }

    @Test
    public void testBeforeHandshakeForwardedBogusAddress() {
        Map<String, Object> attributes = new HashMap<>();

        when(request.getHeaders()).thenReturn(httpHeaders);
        when(request.getRemoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 32000));
        when(httpHeaders.get(X_FORWARDED_FOR_HEADER)).thenReturn(Collections.singletonList("not.a.valid.hostname"));

        assertTrue(interceptor.beforeHandshake(request, response, wsHandler, attributes));

        assertEquals("127.0.0.1", attributes.get(AGONY_REMOTE_IP_KEY));
    }

    @Test
    public void testAfterHandshake() {
        interceptor.afterHandshake(request, response, wsHandler, null);

        verifyZeroInteractions(request, response, wsHandler);
    }
}
