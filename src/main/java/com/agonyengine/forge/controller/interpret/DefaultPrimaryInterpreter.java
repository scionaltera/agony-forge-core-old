package com.agonyengine.forge.controller.interpret;

import com.agonyengine.forge.controller.Input;
import com.agonyengine.forge.controller.Output;
import com.agonyengine.forge.model.Connection;
import com.agonyengine.forge.model.PrimaryConnectionState;
import com.agonyengine.forge.repository.CreatureRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class DefaultPrimaryInterpreter extends BaseInterpreter {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPrimaryInterpreter.class);

    private LoginInterpreterDelegate loginInterpreter;
    private InGameInterpreterDelegate inGameInterpreter;

    @Inject
    public DefaultPrimaryInterpreter(
        CreatureRepository creatureRepository,
        SimpMessagingTemplate simpMessagingTemplate,
        LoginInterpreterDelegate loginInterpreterDelegate,
        InGameInterpreterDelegate inGameInterpreterDelegate) {

        super(creatureRepository, simpMessagingTemplate);

        this.loginInterpreter = loginInterpreterDelegate;
        this.inGameInterpreter = inGameInterpreterDelegate;
    }

    @Override
    public Output interpret(Input input, Connection connection) {
        PrimaryConnectionState primaryState = connection.getPrimaryState();

        switch (primaryState) {
            case LOGIN: return loginInterpreter.interpret(this, input, connection);
            case IN_GAME: return inGameInterpreter.interpret(this, input, connection);
            default:
                LOGGER.error("Reached default state in interpret()!");
                return new Output("[red]Oops! Something went wrong. The error has been logged.");
        }
    }

    @Override
    public Output prompt(Connection connection) {
        PrimaryConnectionState primaryState = connection.getPrimaryState();

        switch (primaryState) {
            case LOGIN: return loginInterpreter.prompt(this, connection);
            case IN_GAME: return inGameInterpreter.prompt(this, connection);
            default:
                LOGGER.error("Reached default state in prompt()!");
                return new Output("[red]Oops! Something went wrong. The error has been logged.");
        }
    }
}
