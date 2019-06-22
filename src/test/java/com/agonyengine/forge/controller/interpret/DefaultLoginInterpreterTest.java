package com.agonyengine.forge.controller.interpret;

import com.agonyengine.forge.controller.Input;
import com.agonyengine.forge.controller.Output;
import com.agonyengine.forge.model.Connection;
import com.agonyengine.forge.model.ConnectionState;
import com.agonyengine.forge.model.Creature;
import com.agonyengine.forge.repository.ConnectionRepository;
import com.agonyengine.forge.repository.CreatureRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

public class DefaultLoginInterpreterTest {
    @Mock
    private UserDetailsManager userDetailsManager;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private ConnectionRepository connectionRepository;

    @Mock
    private CreatureRepository creatureRepository;

    @Mock
    private Session session;

    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;

    @Captor
    private ArgumentCaptor<SecurityContext> securityContextCaptor;

    @Captor
    private ArgumentCaptor<Creature> creatureCaptor;

    private DefaultLoginInterpreter interpreter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(connectionRepository.save(any(Connection.class))).thenAnswer(invocation -> {
            Connection connection = invocation.getArgument(0);

            connection.setId(UUID.randomUUID());

            return connection;
        });

        Creature creature = new Creature();

        when(creatureRepository.findByConnection(any(Connection.class))).thenReturn(Optional.of(creature));

        interpreter = new DefaultLoginInterpreter(
            userDetailsManager,
            authenticationManager,
            sessionRepository,
            connectionRepository,
            creatureRepository,
            simpMessagingTemplate);
    }

    @Test
    public void testPromptAskNew() {
        Connection connection = new Connection();
        connection.setState(ConnectionState.ASK_NEW);

        Output result = interpreter.prompt(connection);

        assertEquals("[default]Create a new character? [y/N]: ", result.toString());
        assertFalse(result.getSecret());
    }

    @Test
    public void testInterpretAskNewNo() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("n");
        connection.setState(ConnectionState.ASK_NEW);

        Output result = interpreter.interpret(input, connection);

        assertEquals("[default]Name: ", result.toString());
        assertFalse(result.getSecret());
        assertEquals(ConnectionState.LOGIN_ASK_NAME, connection.getState());
    }

    @Test
    public void testInterpretLoginAskShortName() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("Dan");
        connection.setState(ConnectionState.LOGIN_ASK_NAME);

        Output result = interpreter.interpret(input, connection);

        assertEquals("[default]Password: ", result.toString());
        assertTrue(result.getSecret());
        assertEquals("Dan", connection.getScratch());
        assertEquals(ConnectionState.LOGIN_ASK_PASSWORD, connection.getState());
    }

    @Test
    public void testInterpretLoginAskLongName() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("Danidanidanidanidanidanidanida");
        connection.setState(ConnectionState.LOGIN_ASK_NAME);

        Output result = interpreter.interpret(input, connection);

        assertEquals("[default]Password: ", result.toString());
        assertTrue(result.getSecret());
        assertEquals("Danidanidanidanidanidanidanida", connection.getScratch());
        assertEquals(ConnectionState.LOGIN_ASK_PASSWORD, connection.getState());
    }

    @Test
    public void testInterpretLoginAskNameWhitespace() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("Dani Filth");
        connection.setState(ConnectionState.LOGIN_ASK_NAME);

        Output result = interpreter.interpret(input, connection);

        assertEquals("[red]Names may not contain whitespace.\n[default]Name: ", result.toString());
        assertFalse(result.getSecret());
        assertNull(connection.getScratch());
        assertEquals(ConnectionState.LOGIN_ASK_NAME, connection.getState());
    }

    @Test
    public void testInterpretLoginAskNameBadChars() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("Dani3");
        connection.setState(ConnectionState.LOGIN_ASK_NAME);

        Output result = interpreter.interpret(input, connection);

        assertEquals("[red]Names may only contain letters.\n[default]Name: ", result.toString());
        assertFalse(result.getSecret());
        assertNull(connection.getScratch());
        assertEquals(ConnectionState.LOGIN_ASK_NAME, connection.getState());
    }

    @Test
    public void testInterpretLoginAskNameTooShort() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("Da");
        connection.setState(ConnectionState.LOGIN_ASK_NAME);

        Output result = interpreter.interpret(input, connection);

        assertEquals("[red]Names must be at least 3 letters long.\n[default]Name: ", result.toString());
        assertFalse(result.getSecret());
        assertNull(connection.getScratch());
        assertEquals(ConnectionState.LOGIN_ASK_NAME, connection.getState());
    }

    @Test
    public void testInterpretLoginAskNameTooLong() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("Danidanidanidanidanidanidanidan");
        connection.setState(ConnectionState.LOGIN_ASK_NAME);

        Output result = interpreter.interpret(input, connection);

        assertEquals("[red]Names may not be longer than 30 letters.\n[default]Name: ", result.toString());
        assertFalse(result.getSecret());
        assertNull(connection.getScratch());
        assertEquals(ConnectionState.LOGIN_ASK_NAME, connection.getState());
    }

    @Test
    public void testInterpretLoginAskNameFirstCaps() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("dani");
        connection.setState(ConnectionState.LOGIN_ASK_NAME);

        Output result = interpreter.interpret(input, connection);

        assertEquals("[red]Names must begin with an upper case letter.\n[default]Name: ", result.toString());
        assertFalse(result.getSecret());
        assertNull(connection.getScratch());
        assertEquals(ConnectionState.LOGIN_ASK_NAME, connection.getState());
    }

    @Test
    public void testInterpretLoginAskNameOtherCaps() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("DAni");
        connection.setState(ConnectionState.LOGIN_ASK_NAME);

        Output result = interpreter.interpret(input, connection);

        assertEquals("[red]Names must not contain upper case letters other than the first.\n[default]Name: ", result.toString());
        assertFalse(result.getSecret());
        assertNull(connection.getScratch());
        assertEquals(ConnectionState.LOGIN_ASK_NAME, connection.getState());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInterpretLoginAskPassword() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("Not!A_Real123Password");
        connection.setScratch("Dani");
        connection.setState(ConnectionState.LOGIN_ASK_PASSWORD);
        connection.setHttpSessionId(UUID.randomUUID().toString());

        when(authenticationManager.authenticate(any(Authentication.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionRepository.findById(anyString())).thenReturn(session);

        Output result = interpreter.interpret(input, connection);

        verify(sessionRepository).findById(anyString());
        verify(session).setAttribute(eq(SPRING_SECURITY_CONTEXT_KEY), securityContextCaptor.capture());
        verify(sessionRepository).save(session);
        verify(creatureRepository).save(creatureCaptor.capture());

        assertEquals("[yellow]Welcome back, Dani!\n\n[default]Dani> ", result.toString());
        assertFalse(result.getSecret());
        assertEquals(ConnectionState.IN_GAME, connection.getState());

        SecurityContext securityContext = securityContextCaptor.getValue();
        Authentication authentication = securityContext.getAuthentication();

        assertEquals("Dani", authentication.getPrincipal());
        assertEquals("Not!A_Real123Password", authentication.getCredentials());

        Creature creature = creatureCaptor.getValue();

        assertEquals("Dani", creature.getName());
        assertEquals(connection, creature.getConnection());
    }

    @Test
    public void testInterpretLoginAskPasswordBadCredentials() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("password");
        connection.setScratch("Dani");
        connection.setState(ConnectionState.LOGIN_ASK_PASSWORD);
        connection.setHttpSessionId(UUID.randomUUID().toString());

        when(authenticationManager.authenticate(any(Authentication.class))).thenThrow(new BadCredentialsException("Boom!"));

        Output result = interpreter.interpret(input, connection);

        verifyZeroInteractions(sessionRepository, session);

        assertEquals("[red]Sorry! Please try again!\n[default]Create a new character? [y/N]: ", result.toString());
        assertFalse(result.getSecret());
        assertEquals(ConnectionState.ASK_NEW, connection.getState());
    }

    @Test
    public void testInterpretLoginAskPasswordTooShort() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("Not!A_R");
        connection.setScratch("Dani");
        connection.setState(ConnectionState.LOGIN_ASK_PASSWORD);
        connection.setHttpSessionId(UUID.randomUUID().toString());

        Output result = interpreter.interpret(input, connection);

        verifyZeroInteractions(sessionRepository, session);

        assertEquals("[red]Passwords must be at least 8 characters.\n[default]Password: ", result.toString());
        assertTrue(result.getSecret());
        assertEquals(ConnectionState.LOGIN_ASK_PASSWORD, connection.getState());
    }

    @Test
    public void testInterpretAskNewYes() {
        Input input = new Input();
        Connection connection = new Connection();

        connection.setState(ConnectionState.ASK_NEW);

        input.setInput("y");

        Output result = interpreter.interpret(input, connection);

        assertEquals("[default]Please choose a name: ", result.toString());
        assertFalse(result.getSecret());
        assertEquals(ConnectionState.CREATE_CHOOSE_NAME, connection.getState());
    }

    @Test
    public void testInterpretCreateChooseShortName() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("Dan");
        connection.setState(ConnectionState.CREATE_CHOOSE_NAME);

        Output result = interpreter.interpret(input, connection);

        assertEquals("[default]Are you sure 'Dan' is the name you want? [y/N]: ", result.toString());
        assertFalse(result.getSecret());
        assertEquals("Dan", connection.getScratch());
        assertEquals(ConnectionState.CREATE_CONFIRM_NAME, connection.getState());
    }

    @Test
    public void testInterpretCreateChooseLongName() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("Danidanidanidanidanidanidanida");
        connection.setState(ConnectionState.CREATE_CHOOSE_NAME);

        Output result = interpreter.interpret(input, connection);

        assertEquals("[default]Are you sure 'Danidanidanidanidanidanidanida' is the name you want? [y/N]: ", result.toString());
        assertFalse(result.getSecret());
        assertEquals("Danidanidanidanidanidanidanida", connection.getScratch());
        assertEquals(ConnectionState.CREATE_CONFIRM_NAME, connection.getState());
    }

    @Test
    public void testInterpretCreateChooseNameWhitespace() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("Dani Filth");
        connection.setState(ConnectionState.CREATE_CHOOSE_NAME);

        Output result = interpreter.interpret(input, connection);

        assertEquals("[red]Names may not contain whitespace.\n[default]Please choose a name: ", result.toString());
        assertFalse(result.getSecret());
        assertNull(connection.getScratch());
        assertEquals(ConnectionState.CREATE_CHOOSE_NAME, connection.getState());
    }

    @Test
    public void testInterpretCreateChooseNameNumbers() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("Dani3");
        connection.setState(ConnectionState.CREATE_CHOOSE_NAME);

        Output result = interpreter.interpret(input, connection);

        assertEquals("[red]Names may only contain letters.\n[default]Please choose a name: ", result.toString());
        assertFalse(result.getSecret());
        assertNull(connection.getScratch());
        assertEquals(ConnectionState.CREATE_CHOOSE_NAME, connection.getState());
    }

    @Test
    public void testInterpretCreateChooseNameTooShort() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("Da");
        connection.setState(ConnectionState.CREATE_CHOOSE_NAME);

        Output result = interpreter.interpret(input, connection);

        assertEquals("[red]Names must be at least 3 letters long.\n[default]Please choose a name: ", result.toString());
        assertFalse(result.getSecret());
        assertNull(connection.getScratch());
        assertEquals(ConnectionState.CREATE_CHOOSE_NAME, connection.getState());
    }

    @Test
    public void testInterpretCreateChooseNameTooLong() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("Danidanidanidanidanidanidanidan");
        connection.setState(ConnectionState.CREATE_CHOOSE_NAME);

        Output result = interpreter.interpret(input, connection);

        assertEquals("[red]Names may not be longer than 30 letters.\n[default]Please choose a name: ", result.toString());
        assertFalse(result.getSecret());
        assertNull(connection.getScratch());
        assertEquals(ConnectionState.CREATE_CHOOSE_NAME, connection.getState());
    }

    @Test
    public void testInterpretCreateChooseNameFirstCaps() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("dani");
        connection.setState(ConnectionState.CREATE_CHOOSE_NAME);

        Output result = interpreter.interpret(input, connection);

        assertEquals("[red]Names must begin with an upper case letter.\n[default]Please choose a name: ", result.toString());
        assertFalse(result.getSecret());
        assertNull(connection.getScratch());
        assertEquals(ConnectionState.CREATE_CHOOSE_NAME, connection.getState());
    }

    @Test
    public void testInterpretCreateChooseNameOtherCaps() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("DAni");
        connection.setState(ConnectionState.CREATE_CHOOSE_NAME);

        Output result = interpreter.interpret(input, connection);

        assertEquals("[red]Names must not contain upper case letters other than the first.\n[default]Please choose a name: ", result.toString());
        assertFalse(result.getSecret());
        assertNull(connection.getScratch());
        assertEquals(ConnectionState.CREATE_CHOOSE_NAME, connection.getState());
    }

    @Test
    public void testInterpretCreateChooseNameUserAlreadyExists() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("Dani");
        connection.setState(ConnectionState.CREATE_CHOOSE_NAME);

        when(userDetailsManager.userExists(eq("Dani"))).thenReturn(true);

        Output result = interpreter.interpret(input, connection);

        assertEquals("[red]That name is already in use. Please try another!\n[default]Please choose a name: ", result.toString());
        assertFalse(result.getSecret());
        assertNull(connection.getScratch());
        assertEquals(ConnectionState.CREATE_CHOOSE_NAME, connection.getState());
    }

    @Test
    public void testInterpretCreateConfirmNameNo() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("n");
        connection.setState(ConnectionState.CREATE_CONFIRM_NAME);

        Output result = interpreter.interpret(input, connection);

        assertEquals("[default]Please choose a name: ", result.toString());
        assertFalse(result.getSecret());
        assertEquals(ConnectionState.CREATE_CHOOSE_NAME, connection.getState());
    }

    @Test
    public void testInterpretCreateConfirmNameYes() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("y");
        connection.setState(ConnectionState.CREATE_CONFIRM_NAME);

        Output result = interpreter.interpret(input, connection);

        assertEquals("[default]Please choose a password: ", result.toString());
        assertTrue(result.getSecret());
        assertEquals(ConnectionState.CREATE_CHOOSE_PASSWORD, connection.getState());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInterpretCreateChoosePassword() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("Not!A_Real123Password");
        connection.setScratch("Dani");
        connection.setState(ConnectionState.CREATE_CHOOSE_PASSWORD);
        connection.setHttpSessionId(UUID.randomUUID().toString());

        when(authenticationManager.authenticate(any(Authentication.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionRepository.findById(anyString())).thenReturn(session);

        Output result = interpreter.interpret(input, connection);

        verify(userDetailsManager).createUser(any(User.class));
        verify(sessionRepository).findById(anyString());
        verify(session).setAttribute(eq(SPRING_SECURITY_CONTEXT_KEY), securityContextCaptor.capture());
        verify(sessionRepository).save(session);

        assertEquals("[default]Please confirm your password: ", result.toString());
        assertTrue(result.getSecret());
        assertEquals(ConnectionState.CREATE_CONFIRM_PASSWORD, connection.getState());

        SecurityContext securityContext = securityContextCaptor.getValue();
        Authentication authentication = securityContext.getAuthentication();

        assertEquals("Dani", authentication.getPrincipal());
        assertEquals("Not!A_Real123Password", authentication.getCredentials());
    }

    @Test
    public void testInterpretCreateChoosePasswordTooShort() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("Not!A_R");
        connection.setScratch("Dani");
        connection.setState(ConnectionState.CREATE_CHOOSE_PASSWORD);
        connection.setHttpSessionId(UUID.randomUUID().toString());

        when(authenticationManager.authenticate(any(Authentication.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionRepository.findById(anyString())).thenReturn(session);

        Output result = interpreter.interpret(input, connection);

        verifyZeroInteractions(sessionRepository, session);

        assertEquals("[red]Passwords must be at least 8 characters.\n[default]Please choose a password: ", result.toString());
        assertTrue(result.getSecret());
        assertEquals(ConnectionState.CREATE_CHOOSE_PASSWORD, connection.getState());
    }

    @Test
    public void testInterpretCreateChoosePasswordSomethingBad() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("Not!A_Real123Password");
        connection.setScratch("Dani");
        connection.setState(ConnectionState.CREATE_CHOOSE_PASSWORD);
        connection.setHttpSessionId(UUID.randomUUID().toString());

        when(authenticationManager.authenticate(any(Authentication.class))).thenThrow(new BadCredentialsException("Boom!"));
        when(sessionRepository.findById(anyString())).thenReturn(session);

        Output result = interpreter.interpret(input, connection);

        verifyZeroInteractions(sessionRepository, session);

        assertEquals("[red]Oops! Something bad happened. The error has been logged.\n[default]Please choose a password: ", result.toString());
        assertTrue(result.getSecret());
        assertEquals(ConnectionState.CREATE_CHOOSE_PASSWORD, connection.getState());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInterpretCreateConfirmPassword() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("Not!A_Real123Password");
        connection.setScratch("Dani");
        connection.setState(ConnectionState.CREATE_CONFIRM_PASSWORD);
        connection.setHttpSessionId(UUID.randomUUID().toString());

        when(authenticationManager.authenticate(any(Authentication.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionRepository.findById(anyString())).thenReturn(session);

        Output result = interpreter.interpret(input, connection);

        verify(sessionRepository).findById(anyString());
        verify(session).setAttribute(eq(SPRING_SECURITY_CONTEXT_KEY), securityContextCaptor.capture());
        verify(sessionRepository).save(session);
        verify(creatureRepository).save(creatureCaptor.capture());

        assertEquals("[yellow]Welcome, Dani!\n\n[default]Dani> ", result.toString());
        assertFalse(result.getSecret());
        assertEquals(ConnectionState.IN_GAME, connection.getState());

        SecurityContext securityContext = securityContextCaptor.getValue();
        Authentication authentication = securityContext.getAuthentication();

        assertEquals("Dani", authentication.getPrincipal());
        assertEquals("Not!A_Real123Password", authentication.getCredentials());

        Creature creature = creatureCaptor.getValue();

        assertEquals("Dani", creature.getName());
        assertEquals(connection, creature.getConnection());
    }

    @Test
    public void testInterpretCreateConfirmPasswordTooShort() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("Not!A_R");
        connection.setScratch("Dani");
        connection.setState(ConnectionState.CREATE_CONFIRM_PASSWORD);
        connection.setHttpSessionId(UUID.randomUUID().toString());

        when(authenticationManager.authenticate(any(Authentication.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionRepository.findById(anyString())).thenReturn(session);

        Output result = interpreter.interpret(input, connection);

        verifyZeroInteractions(sessionRepository, session);

        assertEquals("[red]Passwords must be at least 8 characters.\n[default]Please confirm your password: ", result.toString());
        assertTrue(result.getSecret());
        assertEquals(ConnectionState.CREATE_CONFIRM_PASSWORD, connection.getState());
    }

    @Test
    public void testInterpretCreateConfirmPasswordSomethingBad() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("Not!A_Real123Password");
        connection.setScratch("Dani");
        connection.setState(ConnectionState.CREATE_CONFIRM_PASSWORD);
        connection.setHttpSessionId(UUID.randomUUID().toString());

        when(authenticationManager.authenticate(any(Authentication.class))).thenThrow(new BadCredentialsException("Boom!"));
        when(sessionRepository.findById(anyString())).thenReturn(session);

        Output result = interpreter.interpret(input, connection);

        verifyZeroInteractions(sessionRepository, session);
        verify(userDetailsManager).deleteUser(eq(connection.getScratch()));

        assertEquals("[red]Passwords do not match. Please try again!\n[default]Please choose a password: ", result.toString());
        assertTrue(result.getSecret());
        assertEquals(ConnectionState.CREATE_CHOOSE_PASSWORD, connection.getState());
    }

    @Test
    public void testInterpretLoggedIn() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("Hello world!");
        connection.setScratch("Dani");
        connection.setState(ConnectionState.IN_GAME);

        Creature creatureA = new Creature();
        Connection connectionA = new Connection();

        connectionA.setSessionUsername("CreatureA");
        connectionA.setScratch("CreatureA");
        connectionA.setState(ConnectionState.IN_GAME);
        creatureA.setConnection(connectionA);

        when(creatureRepository.findByConnectionIsNotNull()).thenReturn(Stream.of(creatureA));

        Output result = interpreter.interpret(input, connection);

        verify(simpMessagingTemplate).convertAndSendToUser(anyString(), anyString(), any(Output.class));

        assertEquals("[green]You gossip 'Hello world![green]'\n\n[default]Dani> ", result.toString());
    }

    @Test
    public void testInterpretLoggedInNoCreature() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("Hello world!");
        connection.setScratch("Dani");
        connection.setState(ConnectionState.IN_GAME);

        when(creatureRepository
            .findByConnection(any(Connection.class)))
            .thenReturn(Optional.empty());

        try {
            interpreter.interpret(input, connection);
            fail("Required exception was not thrown.");
        } catch (NullPointerException e) {
            assertTrue(e.getMessage().startsWith("Unable to find Creature for Connection "));
        }
    }
}
