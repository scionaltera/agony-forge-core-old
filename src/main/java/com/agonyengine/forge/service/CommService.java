package com.agonyengine.forge.service;

import com.agonyengine.forge.controller.Output;
import com.agonyengine.forge.model.Creature;
import com.agonyengine.forge.repository.CreatureRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

@Component
public class CommService {
    private CreatureRepository creatureRepository;
    private SimpMessagingTemplate simpMessagingTemplate;

    @Inject
    public CommService(CreatureRepository creatureRepository, SimpMessagingTemplate simpMessagingTemplate) {
        this.creatureRepository = creatureRepository;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    public void echo(Creature target, Output message) {
        if (target.getConnection() == null || target.getConnection().getSessionId() == null) {
            return;
        }

        addPrompt(message);

        simpMessagingTemplate.convertAndSendToUser(target.getConnection().getSessionUsername(), "/queue/output", message);
    }

    public void echoToWorld(Output message, Creature ... exclude) {
        List<Creature> excludeList = Arrays.asList(exclude);

        creatureRepository.findAll()
            .stream()
            .filter(target -> target.getConnection() != null)
            .filter(target -> !excludeList.contains(target))
            .forEach(target -> simpMessagingTemplate.convertAndSendToUser(target.getConnection().getSessionUsername(), "/queue/output", message));
    }

    // TODO prompt should come from the interpreter
    private void addPrompt(Output output) {
        output.append("", "[default]> ");
    }
}
