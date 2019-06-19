package com.agonyengine.forge.controller;

import com.agonyengine.forge.model.Creature;
import com.agonyengine.forge.repository.CreatureRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import javax.inject.Inject;
import java.util.Map;

import static com.agonyengine.forge.controller.ControllerConstants.*;

@Component
public class SessionDisconnectListener implements ApplicationListener<SessionDisconnectEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SessionDisconnectListener.class);

    private CreatureRepository creatureRepository;

    @Inject
    public SessionDisconnectListener(CreatureRepository creatureRepository) {
        this.creatureRepository = creatureRepository;
    }

    @Override
    public void onApplicationEvent(SessionDisconnectEvent event) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> attributes = headerAccessor.getSessionAttributes();

        if (attributes != null) {
            Creature creature = creatureRepository.findByConnectionSessionUsernameAndConnectionSessionId(
                (String) attributes.get(AGONY_STOMP_PRINCIPAL_KEY),
                (String) attributes.get(AGONY_STOMP_SESSION_KEY));

            creatureRepository.delete(creature);
        }

        LOGGER.info("Lost connection from {}", attributes == null ? "(unknown)" : attributes.get(AGONY_REMOTE_IP_KEY));
    }
}
