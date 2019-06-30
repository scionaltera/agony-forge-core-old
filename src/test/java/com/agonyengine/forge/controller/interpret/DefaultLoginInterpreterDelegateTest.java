package com.agonyengine.forge.controller.interpret;

import com.agonyengine.forge.config.LoginConfiguration;
import com.agonyengine.forge.controller.Input;
import com.agonyengine.forge.controller.Output;
import com.agonyengine.forge.model.Connection;
import com.agonyengine.forge.model.Creature;
import com.agonyengine.forge.repository.ConnectionRepository;
import com.agonyengine.forge.repository.CreatureRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
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

import static com.agonyengine.forge.model.DefaultLoginConnectionState.*;
import static com.agonyengine.forge.model.PrimaryConnectionState.IN_GAME;
import static com.agonyengine.forge.model.PrimaryConnectionState.LOGIN;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

public class DefaultLoginInterpreterDelegateTest {
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
    private Interpreter primary;

    @Captor
    private ArgumentCaptor<SecurityContext> securityContextCaptor;

    @Captor
    private ArgumentCaptor<Creature> creatureCaptor;

    private DefaultLoginInterpreterDelegate interpreter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        LoginConfiguration loginConfiguration = new LoginConfigurationBuilder().build();

        when(primary.prompt(any(Connection.class))).thenAnswer(invocation -> {
            Connection connection = invocation.getArgument(0);

            if (LOGIN.equals(connection.getPrimaryState())) {
                return interpreter.prompt(primary, connection);
            } else {
                return new Output("", "[default]Dani> ");
            }
        });

        when(connectionRepository.save(any(Connection.class))).thenAnswer(invocation -> {
            Connection connection = invocation.getArgument(0);

            connection.setId(UUID.randomUUID());

            return connection;
        });

        Creature creature = new Creature();

        when(creatureRepository.findByConnection(any(Connection.class))).thenReturn(Optional.of(creature));

        interpreter = new DefaultLoginInterpreterDelegate(
            loginConfiguration,
            userDetailsManager,
            authenticationManager,
            sessionRepository,
            connectionRepository,
            creatureRepository);
    }

    @Test
    public void testPromptBadSecondaryState() {
        Connection connection = new Connection();
        connection.setPrimaryState(LOGIN);
        connection.setSecondaryState("INVALID");

        try {
            interpreter.prompt(primary, connection);

            fail("Required exception was not thrown");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().startsWith("No enum constant"));
        }
    }

    @Test
    public void testInterpretBadSecondaryState() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("input");
        connection.setPrimaryState(LOGIN);
        connection.setSecondaryState("INVALID");

        try {
            interpreter.interpret(primary, input, connection);

            fail("Required exception was not thrown");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().startsWith("No enum constant"));
        }
    }

    @Test
    public void testPromptAskNew() {
        Connection connection = new Connection();
        connection.setPrimaryState(LOGIN);
        connection.setSecondaryState("DEFAULT");

        Output result = interpreter.prompt(primary, connection);

        assertEquals("[default]Create a new character? [y/N]: ", result.toString());
        assertFalse(result.getSecret());
    }

    @Test
    public void testInterpretAskNewNo() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("n");
        connection.setPrimaryState(LOGIN);
        connection.setSecondaryState(DEFAULT.name());

        Output result = interpreter.interpret(primary, input, connection);

        assertEquals("[default]Name: ", result.toString());
        assertFalse(result.getSecret());
        assertEquals(LOGIN, connection.getPrimaryState());
        assertEquals(LOGIN_ASK_NAME.name(), connection.getSecondaryState());
    }

    @Test
    public void testInterpretLoginAskShortName() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("Dan");
        connection.setPrimaryState(LOGIN);
        connection.setSecondaryState(LOGIN_ASK_NAME.name());

        Output result = interpreter.interpret(primary, input, connection);

        assertEquals("[default]Password: ", result.toString());
        assertTrue(result.getSecret());
        assertEquals("Dan", connection.getName());
        assertEquals(LOGIN, connection.getPrimaryState());
        assertEquals(LOGIN_ASK_PASSWORD.name(), connection.getSecondaryState());
    }

    @Test
    public void testInterpretLoginAskLongName() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("Danidanidanidanidanidanidanida");
        connection.setPrimaryState(LOGIN);
        connection.setSecondaryState(LOGIN_ASK_NAME.name());

        Output result = interpreter.interpret(primary, input, connection);

        assertEquals("[default]Password: ", result.toString());
        assertTrue(result.getSecret());
        assertEquals("Danidanidanidanidanidanidanida", connection.getName());
        assertEquals(LOGIN, connection.getPrimaryState());
        assertEquals(LOGIN_ASK_PASSWORD.name(), connection.getSecondaryState());
    }

    @Test
    public void testInterpretLoginAskNameWhitespace() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("Dani Filth");
        connection.setPrimaryState(LOGIN);
        connection.setSecondaryState(LOGIN_ASK_NAME.name());

        Output result = interpreter.interpret(primary, input, connection);

        assertEquals("[red]Names may not contain whitespace.\n[default]Name: ", result.toString());
        assertFalse(result.getSecret());
        assertNull(connection.getName());
        assertEquals(LOGIN, connection.getPrimaryState());
        assertEquals(LOGIN_ASK_NAME.name(), connection.getSecondaryState());
    }

    @Test
    public void testInterpretLoginAskNameBadChars() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("Dani3");
        connection.setPrimaryState(LOGIN);
        connection.setSecondaryState(LOGIN_ASK_NAME.name());

        Output result = interpreter.interpret(primary, input, connection);

        assertEquals("[red]Names may only contain letters.\n[default]Name: ", result.toString());
        assertFalse(result.getSecret());
        assertNull(connection.getName());
        assertEquals(LOGIN, connection.getPrimaryState());
        assertEquals(LOGIN_ASK_NAME.name(), connection.getSecondaryState());
    }

    @Test
    public void testInterpretLoginAskNameTooShort() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("Da");
        connection.setPrimaryState(LOGIN);
        connection.setSecondaryState(LOGIN_ASK_NAME.name());

        Output result = interpreter.interpret(primary, input, connection);

        assertEquals("[red]Names must be at least 3 letters long.\n[default]Name: ", result.toString());
        assertFalse(result.getSecret());
        assertNull(connection.getName());
        assertEquals(LOGIN, connection.getPrimaryState());
        assertEquals(LOGIN_ASK_NAME.name(), connection.getSecondaryState());
    }

    @Test
    public void testInterpretLoginAskNameTooLong() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("Danidanidanidanidanidanidanidan");
        connection.setPrimaryState(LOGIN);
        connection.setSecondaryState(LOGIN_ASK_NAME.name());

        Output result = interpreter.interpret(primary, input, connection);

        assertEquals("[red]Names may not be longer than 30 letters.\n[default]Name: ", result.toString());
        assertFalse(result.getSecret());
        assertNull(connection.getName());
        assertEquals(LOGIN, connection.getPrimaryState());
        assertEquals(LOGIN_ASK_NAME.name(), connection.getSecondaryState());
    }

    @Test
    public void testInterpretLoginAskNameFirstCaps() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("dani");
        connection.setPrimaryState(LOGIN);
        connection.setSecondaryState(LOGIN_ASK_NAME.name());

        Output result = interpreter.interpret(primary, input, connection);

        assertEquals("[red]Names must begin with an upper case letter.\n[default]Name: ", result.toString());
        assertFalse(result.getSecret());
        assertNull(connection.getName());
        assertEquals(LOGIN, connection.getPrimaryState());
        assertEquals(LOGIN_ASK_NAME.name(), connection.getSecondaryState());
    }

    @Test
    public void testInterpretLoginAskNameOtherCaps() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("DAni");
        connection.setPrimaryState(LOGIN);
        connection.setSecondaryState(LOGIN_ASK_NAME.name());

        Output result = interpreter.interpret(primary, input, connection);

        assertEquals("[red]Names must not contain upper case letters other than the first.\n[default]Name: ", result.toString());
        assertFalse(result.getSecret());
        assertNull(connection.getName());
        assertEquals(LOGIN, connection.getPrimaryState());
        assertEquals(LOGIN_ASK_NAME.name(), connection.getSecondaryState());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInterpretLoginAskPassword() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("Not!A_Real123Password");
        connection.setName("Dani");
        connection.setPrimaryState(LOGIN);
        connection.setSecondaryState(LOGIN_ASK_PASSWORD.name());
        connection.setHttpSessionId(UUID.randomUUID().toString());

        when(authenticationManager.authenticate(any(Authentication.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionRepository.findById(anyString())).thenReturn(session);

        Output result = interpreter.interpret(primary, input, connection);

        verify(sessionRepository).findById(anyString());
        verify(session).setAttribute(eq(SPRING_SECURITY_CONTEXT_KEY), securityContextCaptor.capture());
        verify(sessionRepository).save(session);
        verify(creatureRepository).save(creatureCaptor.capture());

        assertEquals("[yellow]Welcome back, Dani!\n\n[default]Dani> ", result.toString());
        assertFalse(result.getSecret());
        assertEquals(IN_GAME, connection.getPrimaryState());

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
        connection.setName("Dani");
        connection.setPrimaryState(LOGIN);
        connection.setSecondaryState(LOGIN_ASK_PASSWORD.name());
        connection.setHttpSessionId(UUID.randomUUID().toString());

        when(authenticationManager.authenticate(any(Authentication.class))).thenThrow(new BadCredentialsException("Boom!"));

        Output result = interpreter.interpret(primary, input, connection);

        verifyZeroInteractions(sessionRepository, session);

        assertEquals("[red]Sorry! Please try again!\n[default]Create a new character? [y/N]: ", result.toString());
        assertFalse(result.getSecret());
        assertEquals(LOGIN, connection.getPrimaryState());
        assertEquals(DEFAULT.name(), connection.getSecondaryState());
    }

    @Test
    public void testInterpretAskNewYes() {
        Input input = new Input();
        Connection connection = new Connection();

        connection.setPrimaryState(LOGIN);
        connection.setSecondaryState(DEFAULT.name());

        input.setInput("y");

        Output result = interpreter.interpret(primary, input, connection);

        assertEquals("[default]Please choose a name: ", result.toString());
        assertFalse(result.getSecret());
        assertEquals(LOGIN, connection.getPrimaryState());
        assertEquals(CREATE_CHOOSE_NAME.name(), connection.getSecondaryState());
    }

    @Test
    public void testInterpretCreateChooseShortName() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("Dan");
        connection.setPrimaryState(LOGIN);
        connection.setSecondaryState(CREATE_CHOOSE_NAME.name());

        Output result = interpreter.interpret(primary, input, connection);

        assertEquals("[default]Are you sure 'Dan' is the name you want? [y/N]: ", result.toString());
        assertFalse(result.getSecret());
        assertEquals("Dan", connection.getName());
        assertEquals(LOGIN, connection.getPrimaryState());
        assertEquals(CREATE_CONFIRM_NAME.name(), connection.getSecondaryState());
    }

    @Test
    public void testInterpretCreateChooseLongName() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("Danidanidanidanidanidanidanida");
        connection.setPrimaryState(LOGIN);
        connection.setSecondaryState(CREATE_CHOOSE_NAME.name());

        Output result = interpreter.interpret(primary, input, connection);

        assertEquals("[default]Are you sure 'Danidanidanidanidanidanidanida' is the name you want? [y/N]: ", result.toString());
        assertFalse(result.getSecret());
        assertEquals("Danidanidanidanidanidanidanida", connection.getName());
        assertEquals(LOGIN, connection.getPrimaryState());
        assertEquals(CREATE_CONFIRM_NAME.name(), connection.getSecondaryState());
    }

    @Test
    public void testInterpretCreateChooseNameWhitespace() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("Dani Filth");
        connection.setPrimaryState(LOGIN);
        connection.setSecondaryState(CREATE_CHOOSE_NAME.name());

        Output result = interpreter.interpret(primary, input, connection);

        assertEquals("[red]Names may not contain whitespace.\n[default]Please choose a name: ", result.toString());
        assertFalse(result.getSecret());
        assertNull(connection.getName());
        assertEquals(LOGIN, connection.getPrimaryState());
        assertEquals(CREATE_CHOOSE_NAME.name(), connection.getSecondaryState());
    }

    @Test
    public void testInterpretCreateChooseNameNumbers() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("Dani3");
        connection.setPrimaryState(LOGIN);
        connection.setSecondaryState(CREATE_CHOOSE_NAME.name());

        Output result = interpreter.interpret(primary, input, connection);

        assertEquals("[red]Names may only contain letters.\n[default]Please choose a name: ", result.toString());
        assertFalse(result.getSecret());
        assertNull(connection.getName());
        assertEquals(LOGIN, connection.getPrimaryState());
        assertEquals(CREATE_CHOOSE_NAME.name(), connection.getSecondaryState());
    }

    @Test
    public void testInterpretCreateChooseNameTooShort() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("Da");
        connection.setPrimaryState(LOGIN);
        connection.setSecondaryState(CREATE_CHOOSE_NAME.name());

        Output result = interpreter.interpret(primary, input, connection);

        assertEquals("[red]Names must be at least 3 letters long.\n[default]Please choose a name: ", result.toString());
        assertFalse(result.getSecret());
        assertNull(connection.getName());
        assertEquals(LOGIN, connection.getPrimaryState());
        assertEquals(CREATE_CHOOSE_NAME.name(), connection.getSecondaryState());
    }

    @Test
    public void testInterpretCreateChooseNameTooLong() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("Danidanidanidanidanidanidanidan");
        connection.setPrimaryState(LOGIN);
        connection.setSecondaryState(CREATE_CHOOSE_NAME.name());

        Output result = interpreter.interpret(primary, input, connection);

        assertEquals("[red]Names may not be longer than 30 letters.\n[default]Please choose a name: ", result.toString());
        assertFalse(result.getSecret());
        assertNull(connection.getName());
        assertEquals(LOGIN, connection.getPrimaryState());
        assertEquals(CREATE_CHOOSE_NAME.name(), connection.getSecondaryState());
    }

    @Test
    public void testInterpretCreateChooseNameFirstCaps() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("dani");
        connection.setPrimaryState(LOGIN);
        connection.setSecondaryState(CREATE_CHOOSE_NAME.name());

        Output result = interpreter.interpret(primary, input, connection);

        assertEquals("[red]Names must begin with an upper case letter.\n[default]Please choose a name: ", result.toString());
        assertFalse(result.getSecret());
        assertNull(connection.getName());
        assertEquals(LOGIN, connection.getPrimaryState());
        assertEquals(CREATE_CHOOSE_NAME.name(), connection.getSecondaryState());
    }

    @Test
    public void testInterpretCreateChooseNameOtherCaps() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("DAni");
        connection.setPrimaryState(LOGIN);
        connection.setSecondaryState(CREATE_CHOOSE_NAME.name());

        Output result = interpreter.interpret(primary, input, connection);

        assertEquals("[red]Names must not contain upper case letters other than the first.\n[default]Please choose a name: ", result.toString());
        assertFalse(result.getSecret());
        assertNull(connection.getName());
        assertEquals(LOGIN, connection.getPrimaryState());
        assertEquals(CREATE_CHOOSE_NAME.name(), connection.getSecondaryState());
    }

    @Test
    public void testInterpretCreateChooseNameUserAlreadyExists() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("Dani");
        connection.setPrimaryState(LOGIN);
        connection.setSecondaryState(CREATE_CHOOSE_NAME.name());

        when(userDetailsManager.userExists(eq("Dani"))).thenReturn(true);

        Output result = interpreter.interpret(primary, input, connection);

        assertEquals("[red]That name is already in use. Please try another!\n[default]Please choose a name: ", result.toString());
        assertFalse(result.getSecret());
        assertNull(connection.getName());
        assertEquals(LOGIN, connection.getPrimaryState());
        assertEquals(CREATE_CHOOSE_NAME.name(), connection.getSecondaryState());
    }

    @Test
    public void testInterpretCreateConfirmNameNo() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("n");
        connection.setPrimaryState(LOGIN);
        connection.setSecondaryState(CREATE_CONFIRM_NAME.name());

        Output result = interpreter.interpret(primary, input, connection);

        assertEquals("[default]Please choose a name: ", result.toString());
        assertFalse(result.getSecret());
        assertEquals(LOGIN, connection.getPrimaryState());
        assertEquals(CREATE_CHOOSE_NAME.name(), connection.getSecondaryState());
    }

    @Test
    public void testInterpretCreateConfirmNameYes() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("y");
        connection.setPrimaryState(LOGIN);
        connection.setSecondaryState(CREATE_CONFIRM_NAME.name());

        Output result = interpreter.interpret(primary, input, connection);

        assertEquals("[default]Please choose a password: ", result.toString());
        assertTrue(result.getSecret());
        assertEquals(LOGIN, connection.getPrimaryState());
        assertEquals(CREATE_CHOOSE_PASSWORD.name(), connection.getSecondaryState());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInterpretCreateChoosePassword() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("Not!A_PW");
        connection.setName("Dani");
        connection.setPrimaryState(LOGIN);
        connection.setSecondaryState(CREATE_CHOOSE_PASSWORD.name());
        connection.setHttpSessionId(UUID.randomUUID().toString());

        when(authenticationManager.authenticate(any(Authentication.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionRepository.findById(anyString())).thenReturn(session);

        Output result = interpreter.interpret(primary, input, connection);

        verify(userDetailsManager).createUser(any(User.class));
        verify(sessionRepository).findById(anyString());
        verify(session).setAttribute(eq(SPRING_SECURITY_CONTEXT_KEY), securityContextCaptor.capture());
        verify(sessionRepository).save(session);

        assertEquals("[default]Please confirm your password: ", result.toString());
        assertTrue(result.getSecret());
        assertEquals(LOGIN, connection.getPrimaryState());
        assertEquals(CREATE_CONFIRM_PASSWORD.name(), connection.getSecondaryState());

        SecurityContext securityContext = securityContextCaptor.getValue();
        Authentication authentication = securityContext.getAuthentication();

        assertEquals("Dani", authentication.getPrincipal());
        assertEquals("Not!A_PW", authentication.getCredentials());
    }

    @Test
    public void testInterpretCreateChoosePasswordTooShort() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("Not!A_P");
        connection.setName("Dani");
        connection.setPrimaryState(LOGIN);
        connection.setSecondaryState(CREATE_CHOOSE_PASSWORD.name());
        connection.setHttpSessionId(UUID.randomUUID().toString());

        when(authenticationManager.authenticate(any(Authentication.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionRepository.findById(anyString())).thenReturn(session);

        Output result = interpreter.interpret(primary, input, connection);

        verifyZeroInteractions(sessionRepository, session);

        assertEquals("[red]Passwords must be at least 8 characters.\n[default]Please choose a password: ", result.toString());
        assertTrue(result.getSecret());
        assertEquals(LOGIN, connection.getPrimaryState());
        assertEquals(CREATE_CHOOSE_PASSWORD.name(), connection.getSecondaryState());
    }

    @Test
    public void testInterpretCreateChoosePasswordSomethingBad() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("Not!A_Real123Password");
        connection.setName("Dani");
        connection.setPrimaryState(LOGIN);
        connection.setSecondaryState(CREATE_CHOOSE_PASSWORD.name());
        connection.setHttpSessionId(UUID.randomUUID().toString());

        when(authenticationManager.authenticate(any(Authentication.class))).thenThrow(new BadCredentialsException("Boom!"));
        when(sessionRepository.findById(anyString())).thenReturn(session);

        Output result = interpreter.interpret(primary, input, connection);

        verifyZeroInteractions(sessionRepository, session);

        assertEquals("[red]Oops! Something bad happened. The error has been logged.\n[default]Please choose a password: ", result.toString());
        assertTrue(result.getSecret());
        assertEquals(LOGIN, connection.getPrimaryState());
        assertEquals(CREATE_CHOOSE_PASSWORD.name(), connection.getSecondaryState());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInterpretCreateConfirmPassword() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("Not!A_Real123Password");
        connection.setName("Dani");
        connection.setPrimaryState(LOGIN);
        connection.setSecondaryState(CREATE_CONFIRM_PASSWORD.name());
        connection.setHttpSessionId(UUID.randomUUID().toString());

        when(authenticationManager.authenticate(any(Authentication.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionRepository.findById(anyString())).thenReturn(session);

        Output result = interpreter.interpret(primary, input, connection);

        verify(sessionRepository).findById(anyString());
        verify(session).setAttribute(eq(SPRING_SECURITY_CONTEXT_KEY), securityContextCaptor.capture());
        verify(sessionRepository).save(session);
        verify(creatureRepository).save(creatureCaptor.capture());

        assertEquals("[yellow]Welcome, Dani!\n\n[default]Dani> ", result.toString());
        assertFalse(result.getSecret());
        assertEquals(IN_GAME, connection.getPrimaryState());
        assertEquals("DEFAULT", connection.getSecondaryState());

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
        connection.setName("Dani");
        connection.setPrimaryState(LOGIN);
        connection.setSecondaryState(CREATE_CONFIRM_PASSWORD.name());
        connection.setHttpSessionId(UUID.randomUUID().toString());

        when(authenticationManager.authenticate(any(Authentication.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionRepository.findById(anyString())).thenReturn(session);

        Output result = interpreter.interpret(primary, input, connection);

        verifyZeroInteractions(sessionRepository, session);

        assertEquals("[red]Passwords must be at least 8 characters.\n[default]Please confirm your password: ", result.toString());
        assertTrue(result.getSecret());
        assertEquals(LOGIN, connection.getPrimaryState());
        assertEquals(CREATE_CONFIRM_PASSWORD.name(), connection.getSecondaryState());
    }

    @Test
    public void testInterpretCreateConfirmPasswordSomethingBad() {
        Input input = new Input();
        Connection connection = new Connection();

        input.setInput("Not!A_Real123Password");
        connection.setName("Dani");
        connection.setPrimaryState(LOGIN);
        connection.setSecondaryState(CREATE_CONFIRM_PASSWORD.name());
        connection.setHttpSessionId(UUID.randomUUID().toString());

        when(authenticationManager.authenticate(any(Authentication.class))).thenThrow(new BadCredentialsException("Boom!"));
        when(sessionRepository.findById(anyString())).thenReturn(session);

        Output result = interpreter.interpret(primary, input, connection);

        verifyZeroInteractions(sessionRepository, session);
        verify(userDetailsManager).deleteUser(eq(connection.getName()));

        assertEquals("[red]Passwords do not match. Please try again!\n[default]Please choose a password: ", result.toString());
        assertTrue(result.getSecret());
        assertEquals(LOGIN, connection.getPrimaryState());
        assertEquals(CREATE_CHOOSE_PASSWORD.name(), connection.getSecondaryState());
    }
}
