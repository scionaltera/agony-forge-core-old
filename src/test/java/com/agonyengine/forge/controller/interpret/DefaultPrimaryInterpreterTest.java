package com.agonyengine.forge.controller.interpret;

import com.agonyengine.forge.controller.Input;
import com.agonyengine.forge.controller.Output;
import com.agonyengine.forge.model.Connection;
import com.agonyengine.forge.repository.CreatureRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static com.agonyengine.forge.model.PrimaryConnectionState.IN_GAME;
import static com.agonyengine.forge.model.PrimaryConnectionState.LOGIN;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class DefaultPrimaryInterpreterTest {
    @Mock
    private CreatureRepository creatureRepository;

    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;

    @Mock
    private LoginInterpreterDelegate loginInterpreterDelegate;

    @Mock
    private InGameInterpreterDelegate inGameInterpreterDelegate;

    private DefaultPrimaryInterpreter primary;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        primary = new DefaultPrimaryInterpreter(
            creatureRepository,
            simpMessagingTemplate,
            loginInterpreterDelegate,
            inGameInterpreterDelegate
        );

        // Have to do the expectations after instantiating the class under test
        // in this case because we need a matcher for the class under test.

        when(loginInterpreterDelegate.interpret(eq(primary), any(), any())).thenReturn(new Output("Login!"));
        when(inGameInterpreterDelegate.interpret(eq(primary), any(), any())).thenReturn(new Output("In Game!"));

        when(loginInterpreterDelegate.prompt(eq(primary), any())).thenReturn(new Output("Login Prompt!"));
        when(inGameInterpreterDelegate.prompt(eq(primary), any())).thenReturn(new Output("In Game Prompt!"));
    }

    @Test
    public void testInterpretLogin() {
        Connection connection = new Connection();
        Input input = new Input();

        input.setInput("Input!");
        connection.setPrimaryState(LOGIN);

        Output output = primary.interpret(input, connection);

        verify(loginInterpreterDelegate).interpret(eq(primary), eq(input), eq(connection));
        verifyZeroInteractions(inGameInterpreterDelegate);

        assertEquals("Login!", output.toString());
    }

    @Test
    public void testInterpretInGame() {
        Connection connection = new Connection();
        Input input = new Input();

        input.setInput("Input!");
        connection.setPrimaryState(IN_GAME);

        Output output = primary.interpret(input, connection);

        verify(inGameInterpreterDelegate).interpret(eq(primary), eq(input), eq(connection));
        verifyZeroInteractions(loginInterpreterDelegate);

        assertEquals("In Game!", output.toString());
    }

    @Test
    public void testPromptLogin() {
        Connection connection = new Connection();

        connection.setPrimaryState(LOGIN);

        Output output = primary.prompt(connection);

        verify(loginInterpreterDelegate).prompt(eq(primary), eq(connection));
        verifyZeroInteractions(inGameInterpreterDelegate);

        assertEquals("Login Prompt!", output.toString());
    }

    @Test
    public void testPromptInGame() {
        Connection connection = new Connection();

        connection.setPrimaryState(IN_GAME);

        Output output = primary.prompt(connection);

        verify(inGameInterpreterDelegate).prompt(eq(primary), eq(connection));
        verifyZeroInteractions(loginInterpreterDelegate);

        assertEquals("In Game Prompt!", output.toString());
    }
}
