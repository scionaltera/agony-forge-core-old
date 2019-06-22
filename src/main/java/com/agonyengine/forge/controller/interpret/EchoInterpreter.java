package com.agonyengine.forge.controller.interpret;

import com.agonyengine.forge.controller.Input;
import com.agonyengine.forge.controller.Output;
import com.agonyengine.forge.model.Connection;
import com.agonyengine.forge.repository.CreatureRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class EchoInterpreter extends BaseInterpreter {
    @Inject
    public EchoInterpreter(CreatureRepository creatureRepository, SimpMessagingTemplate simpMessagingTemplate) {
        super(creatureRepository, simpMessagingTemplate);
    }

    @Override
    public Output interpret(Input input, Connection connection) {
        return new Output("[cyan]" + input).append(prompt(connection));
    }

    @Override
    public Output prompt(Connection connection) {
        return new Output("[default]> ");
    }
}
