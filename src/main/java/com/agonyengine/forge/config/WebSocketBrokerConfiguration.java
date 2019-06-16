package com.agonyengine.forge.config;

import com.agonyengine.forge.controller.RemoteIpHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.StompBrokerRelayRegistration;
import org.springframework.messaging.simp.stomp.StompDecoder;
import org.springframework.messaging.simp.stomp.StompReactorNettyCodec;
import org.springframework.messaging.tcp.TcpOperations;
import org.springframework.messaging.tcp.reactor.ReactorNettyCodec;
import org.springframework.messaging.tcp.reactor.ReactorNettyTcpClient;
import org.springframework.session.Session;
import org.springframework.session.web.socket.config.annotation.AbstractSessionWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import javax.inject.Inject;

@EnableWebSocketMessageBroker
@Configuration
public class WebSocketBrokerConfiguration extends AbstractSessionWebSocketMessageBrokerConfigurer<Session> {
    private WebSocketBrokerProperties brokerProperties;

    @Inject
    public WebSocketBrokerConfiguration(WebSocketBrokerProperties brokerProperties) {
        this.brokerProperties = brokerProperties;
    }

    @Override
    protected void configureStompEndpoints(StompEndpointRegistry registry) {
        HttpSessionHandshakeInterceptor httpSessionHandshakeInterceptor = new HttpSessionHandshakeInterceptor();

        httpSessionHandshakeInterceptor.setCreateSession(true);
        registry
            .addEndpoint("/mud")
            .setHandshakeHandler(new UniqueHandshakeHandler())
            .withSockJS()
            .setInterceptors(httpSessionHandshakeInterceptor, new RemoteIpHandshakeInterceptor())
            .setSupressCors(true);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        StompBrokerRelayRegistration relayRegistration = registry
            .setApplicationDestinationPrefixes("/app", "/user")
            .setUserDestinationPrefix("/user")
            .enableStompBrokerRelay("/queue", "/topic")
            .setUserDestinationBroadcast("/topic/user-destination")
            .setUserRegistryBroadcast("/topic/user-registry")
            .setRelayHost(brokerProperties.getHost())
            .setRelayPort(brokerProperties.getPort())
            .setSystemLogin(brokerProperties.getSystemUsername())
            .setSystemPasscode(brokerProperties.getSystemPassword())
            .setClientLogin(brokerProperties.getClientUsername())
            .setClientPasscode(brokerProperties.getClientPassword());

        if (brokerProperties.getSsl()) {
            relayRegistration.setTcpClient(createSslTcpClient());
        }
    }

    private TcpOperations<byte[]> createSslTcpClient() {
        StompDecoder decoder = new StompDecoder();
        ReactorNettyCodec<byte[]> codec = new StompReactorNettyCodec(decoder);

        return new ReactorNettyTcpClient<>(
            builder -> builder
                .host(brokerProperties.getHost())
                .port(brokerProperties.getPort())
                .secure(),
            codec
        );
    }
}
