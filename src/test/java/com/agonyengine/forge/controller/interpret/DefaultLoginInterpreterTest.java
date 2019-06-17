package com.agonyengine.forge.controller.interpret;

import com.agonyengine.forge.controller.Input;
import com.agonyengine.forge.controller.Output;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.agonyengine.forge.controller.interpret.DefaultLoginInterpreter.CURRENT_STATE_KEY;
import static com.agonyengine.forge.controller.interpret.DefaultLoginInterpreter.TEMP_NAME_KEY;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;
import static org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor.HTTP_SESSION_ID_ATTR_NAME;

public class DefaultLoginInterpreterTest {
    @Mock
    private UserDetailsManager userDetailsManager;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private Session session;

    @Captor
    private ArgumentCaptor<SecurityContext> securityContextCaptor;

    private DefaultLoginInterpreter interpreter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        interpreter = new DefaultLoginInterpreter(
            userDetailsManager,
            authenticationManager,
            sessionRepository);
    }

    @Test
    public void testPromptAskNew() {
        Output result = interpreter.prompt(new HashMap<>());

        assertEquals("[default]Create a new character? [y/N]: ", result.toString());
        assertFalse(result.getSecret());
    }

    @Test
    public void testInterpretAskNewNo() {
        Input input = new Input();
        Map<String, Object> attributes = new HashMap<>();

        input.setInput("n");

        Output result = interpreter.interpret(input, attributes);

        assertEquals("[default]Name: ", result.toString());
        assertFalse(result.getSecret());
        assertEquals(DefaultLoginInterpreter.InterpreterState.LOGIN_ASK_NAME, attributes.get(CURRENT_STATE_KEY));
    }

    @Test
    public void testInterpretLoginAskShortName() {
        Input input = new Input();
        Map<String, Object> attributes = new HashMap<>();

        input.setInput("Dan");
        attributes.put(CURRENT_STATE_KEY, DefaultLoginInterpreter.InterpreterState.LOGIN_ASK_NAME);

        Output result = interpreter.interpret(input, attributes);

        assertEquals("[default]Password: ", result.toString());
        assertTrue(result.getSecret());
        assertEquals("Dan", attributes.get(TEMP_NAME_KEY));
        assertEquals(DefaultLoginInterpreter.InterpreterState.LOGIN_ASK_PASSWORD, attributes.get(CURRENT_STATE_KEY));
    }

    @Test
    public void testInterpretLoginAskLongName() {
        Input input = new Input();
        Map<String, Object> attributes = new HashMap<>();

        input.setInput("Danidanidanidanidanidanidanida");
        attributes.put(CURRENT_STATE_KEY, DefaultLoginInterpreter.InterpreterState.LOGIN_ASK_NAME);

        Output result = interpreter.interpret(input, attributes);

        assertEquals("[default]Password: ", result.toString());
        assertTrue(result.getSecret());
        assertEquals("Danidanidanidanidanidanidanida", attributes.get(TEMP_NAME_KEY));
        assertEquals(DefaultLoginInterpreter.InterpreterState.LOGIN_ASK_PASSWORD, attributes.get(CURRENT_STATE_KEY));
    }

    @Test
    public void testInterpretLoginAskNameWhitespace() {
        Input input = new Input();
        Map<String, Object> attributes = new HashMap<>();

        input.setInput("Dani Filth");
        attributes.put(CURRENT_STATE_KEY, DefaultLoginInterpreter.InterpreterState.LOGIN_ASK_NAME);

        Output result = interpreter.interpret(input, attributes);

        assertEquals("[red]Names may not contain whitespace.\n[default]Name: ", result.toString());
        assertFalse(result.getSecret());
        assertNull(attributes.get(TEMP_NAME_KEY));
        assertEquals(DefaultLoginInterpreter.InterpreterState.LOGIN_ASK_NAME, attributes.get(CURRENT_STATE_KEY));
    }

    @Test
    public void testInterpretLoginAskNameBadChars() {
        Input input = new Input();
        Map<String, Object> attributes = new HashMap<>();

        input.setInput("Dani3");
        attributes.put(CURRENT_STATE_KEY, DefaultLoginInterpreter.InterpreterState.LOGIN_ASK_NAME);

        Output result = interpreter.interpret(input, attributes);

        assertEquals("[red]Names may only contain letters.\n[default]Name: ", result.toString());
        assertFalse(result.getSecret());
        assertNull(attributes.get(TEMP_NAME_KEY));
        assertEquals(DefaultLoginInterpreter.InterpreterState.LOGIN_ASK_NAME, attributes.get(CURRENT_STATE_KEY));
    }

    @Test
    public void testInterpretLoginAskNameTooShort() {
        Input input = new Input();
        Map<String, Object> attributes = new HashMap<>();

        input.setInput("Da");
        attributes.put(CURRENT_STATE_KEY, DefaultLoginInterpreter.InterpreterState.LOGIN_ASK_NAME);

        Output result = interpreter.interpret(input, attributes);

        assertEquals("[red]Names must be at least 3 letters long.\n[default]Name: ", result.toString());
        assertFalse(result.getSecret());
        assertNull(attributes.get(TEMP_NAME_KEY));
        assertEquals(DefaultLoginInterpreter.InterpreterState.LOGIN_ASK_NAME, attributes.get(CURRENT_STATE_KEY));
    }

    @Test
    public void testInterpretLoginAskNameTooLong() {
        Input input = new Input();
        Map<String, Object> attributes = new HashMap<>();

        input.setInput("Danidanidanidanidanidanidanidan");
        attributes.put(CURRENT_STATE_KEY, DefaultLoginInterpreter.InterpreterState.LOGIN_ASK_NAME);

        Output result = interpreter.interpret(input, attributes);

        assertEquals("[red]Names may not be longer than 30 letters.\n[default]Name: ", result.toString());
        assertFalse(result.getSecret());
        assertNull(attributes.get(TEMP_NAME_KEY));
        assertEquals(DefaultLoginInterpreter.InterpreterState.LOGIN_ASK_NAME, attributes.get(CURRENT_STATE_KEY));
    }

    @Test
    public void testInterpretLoginAskNameFirstCaps() {
        Input input = new Input();
        Map<String, Object> attributes = new HashMap<>();

        input.setInput("dani");
        attributes.put(CURRENT_STATE_KEY, DefaultLoginInterpreter.InterpreterState.LOGIN_ASK_NAME);

        Output result = interpreter.interpret(input, attributes);

        assertEquals("[red]Names must begin with an upper case letter.\n[default]Name: ", result.toString());
        assertFalse(result.getSecret());
        assertNull(attributes.get(TEMP_NAME_KEY));
        assertEquals(DefaultLoginInterpreter.InterpreterState.LOGIN_ASK_NAME, attributes.get(CURRENT_STATE_KEY));
    }

    @Test
    public void testInterpretLoginAskNameOtherCaps() {
        Input input = new Input();
        Map<String, Object> attributes = new HashMap<>();

        input.setInput("DAni");
        attributes.put(CURRENT_STATE_KEY, DefaultLoginInterpreter.InterpreterState.LOGIN_ASK_NAME);

        Output result = interpreter.interpret(input, attributes);

        assertEquals("[red]Names must not contain upper case letters other than the first.\n[default]Name: ", result.toString());
        assertFalse(result.getSecret());
        assertNull(attributes.get(TEMP_NAME_KEY));
        assertEquals(DefaultLoginInterpreter.InterpreterState.LOGIN_ASK_NAME, attributes.get(CURRENT_STATE_KEY));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInterpretLoginAskPassword() {
        Input input = new Input();
        Map<String, Object> attributes = new HashMap<>();

        input.setInput("Not!A_Real123Password");
        attributes.put(TEMP_NAME_KEY, "Dani");
        attributes.put(CURRENT_STATE_KEY, DefaultLoginInterpreter.InterpreterState.LOGIN_ASK_PASSWORD);
        attributes.put(HTTP_SESSION_ID_ATTR_NAME, UUID.randomUUID().toString());

        when(authenticationManager.authenticate(any(Authentication.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionRepository.findById(anyString())).thenReturn(session);

        Output result = interpreter.interpret(input, attributes);

        verify(sessionRepository).findById(anyString());
        verify(session).setAttribute(eq(SPRING_SECURITY_CONTEXT_KEY), securityContextCaptor.capture());
        verify(sessionRepository).save(session);

        assertEquals("[yellow]Welcome, Dani!\n[default]Dani> ", result.toString());
        assertFalse(result.getSecret());
        assertEquals(DefaultLoginInterpreter.InterpreterState.LOGGED_IN, attributes.get(CURRENT_STATE_KEY));

        SecurityContext securityContext = securityContextCaptor.getValue();
        Authentication authentication = securityContext.getAuthentication();

        assertEquals("Dani", authentication.getPrincipal());
        assertEquals("Not!A_Real123Password", authentication.getCredentials());
    }

    @Test
    public void testInterpretLoginAskPasswordBadCredentials() {
        Input input = new Input();
        Map<String, Object> attributes = new HashMap<>();

        input.setInput("password");
        attributes.put(TEMP_NAME_KEY, "Dani");
        attributes.put(CURRENT_STATE_KEY, DefaultLoginInterpreter.InterpreterState.LOGIN_ASK_PASSWORD);
        attributes.put(HTTP_SESSION_ID_ATTR_NAME, UUID.randomUUID().toString());

        when(authenticationManager.authenticate(any(Authentication.class))).thenThrow(new BadCredentialsException("Boom!"));

        Output result = interpreter.interpret(input, attributes);

        verifyZeroInteractions(sessionRepository, session);

        assertEquals("[red]Sorry! Please try again!\n[default]Create a new character? [y/N]: ", result.toString());
        assertFalse(result.getSecret());
        assertEquals(DefaultLoginInterpreter.InterpreterState.ASK_NEW, attributes.get(CURRENT_STATE_KEY));
    }

    @Test
    public void testInterpretLoginAskPasswordTooShort() {
        Input input = new Input();
        Map<String, Object> attributes = new HashMap<>();

        input.setInput("Not!A_R");
        attributes.put(TEMP_NAME_KEY, "Dani");
        attributes.put(CURRENT_STATE_KEY, DefaultLoginInterpreter.InterpreterState.LOGIN_ASK_PASSWORD);
        attributes.put(HTTP_SESSION_ID_ATTR_NAME, UUID.randomUUID().toString());

        Output result = interpreter.interpret(input, attributes);

        verifyZeroInteractions(sessionRepository, session);

        assertEquals("[red]Passwords must be at least 8 characters.\n[default]Password: ", result.toString());
        assertTrue(result.getSecret());
        assertEquals(DefaultLoginInterpreter.InterpreterState.LOGIN_ASK_PASSWORD, attributes.get(CURRENT_STATE_KEY));
    }

    @Test
    public void testInterpretAskNewYes() {
        Input input = new Input();
        Map<String, Object> attributes = new HashMap<>();

        input.setInput("y");

        Output result = interpreter.interpret(input, attributes);

        assertEquals("[default]Please choose a name: ", result.toString());
        assertFalse(result.getSecret());
        assertEquals(DefaultLoginInterpreter.InterpreterState.CREATE_CHOOSE_NAME, attributes.get(CURRENT_STATE_KEY));
    }

    @Test
    public void testInterpretCreateChooseShortName() {
        Input input = new Input();
        Map<String, Object> attributes = new HashMap<>();

        input.setInput("Dan");
        attributes.put(CURRENT_STATE_KEY, DefaultLoginInterpreter.InterpreterState.CREATE_CHOOSE_NAME);

        Output result = interpreter.interpret(input, attributes);

        assertEquals("[default]Are you sure 'Dan' is the name you want? [y/N]: ", result.toString());
        assertFalse(result.getSecret());
        assertEquals("Dan", attributes.get(TEMP_NAME_KEY));
        assertEquals(DefaultLoginInterpreter.InterpreterState.CREATE_CONFIRM_NAME, attributes.get(CURRENT_STATE_KEY));
    }

    @Test
    public void testInterpretCreateChooseLongName() {
        Input input = new Input();
        Map<String, Object> attributes = new HashMap<>();

        input.setInput("Danidanidanidanidanidanidanida");
        attributes.put(CURRENT_STATE_KEY, DefaultLoginInterpreter.InterpreterState.CREATE_CHOOSE_NAME);

        Output result = interpreter.interpret(input, attributes);

        assertEquals("[default]Are you sure 'Danidanidanidanidanidanidanida' is the name you want? [y/N]: ", result.toString());
        assertFalse(result.getSecret());
        assertEquals("Danidanidanidanidanidanidanida", attributes.get(TEMP_NAME_KEY));
        assertEquals(DefaultLoginInterpreter.InterpreterState.CREATE_CONFIRM_NAME, attributes.get(CURRENT_STATE_KEY));
    }

    @Test
    public void testInterpretCreateChooseNameWhitespace() {
        Input input = new Input();
        Map<String, Object> attributes = new HashMap<>();

        input.setInput("Dani Filth");
        attributes.put(CURRENT_STATE_KEY, DefaultLoginInterpreter.InterpreterState.CREATE_CHOOSE_NAME);

        Output result = interpreter.interpret(input, attributes);

        assertEquals("[red]Names may not contain whitespace.\n[default]Please choose a name: ", result.toString());
        assertFalse(result.getSecret());
        assertNull(attributes.get(TEMP_NAME_KEY));
        assertEquals(DefaultLoginInterpreter.InterpreterState.CREATE_CHOOSE_NAME, attributes.get(CURRENT_STATE_KEY));
    }

    @Test
    public void testInterpretCreateChooseNameNumbers() {
        Input input = new Input();
        Map<String, Object> attributes = new HashMap<>();

        input.setInput("Dani3");
        attributes.put(CURRENT_STATE_KEY, DefaultLoginInterpreter.InterpreterState.CREATE_CHOOSE_NAME);

        Output result = interpreter.interpret(input, attributes);

        assertEquals("[red]Names may only contain letters.\n[default]Please choose a name: ", result.toString());
        assertFalse(result.getSecret());
        assertNull(attributes.get(TEMP_NAME_KEY));
        assertEquals(DefaultLoginInterpreter.InterpreterState.CREATE_CHOOSE_NAME, attributes.get(CURRENT_STATE_KEY));
    }

    @Test
    public void testInterpretCreateChooseNameTooShort() {
        Input input = new Input();
        Map<String, Object> attributes = new HashMap<>();

        input.setInput("Da");
        attributes.put(CURRENT_STATE_KEY, DefaultLoginInterpreter.InterpreterState.CREATE_CHOOSE_NAME);

        Output result = interpreter.interpret(input, attributes);

        assertEquals("[red]Names must be at least 3 letters long.\n[default]Please choose a name: ", result.toString());
        assertFalse(result.getSecret());
        assertNull(attributes.get(TEMP_NAME_KEY));
        assertEquals(DefaultLoginInterpreter.InterpreterState.CREATE_CHOOSE_NAME, attributes.get(CURRENT_STATE_KEY));
    }

    @Test
    public void testInterpretCreateChooseNameTooLong() {
        Input input = new Input();
        Map<String, Object> attributes = new HashMap<>();

        input.setInput("Danidanidanidanidanidanidanidan");
        attributes.put(CURRENT_STATE_KEY, DefaultLoginInterpreter.InterpreterState.CREATE_CHOOSE_NAME);

        Output result = interpreter.interpret(input, attributes);

        assertEquals("[red]Names may not be longer than 30 letters.\n[default]Please choose a name: ", result.toString());
        assertFalse(result.getSecret());
        assertNull(attributes.get(TEMP_NAME_KEY));
        assertEquals(DefaultLoginInterpreter.InterpreterState.CREATE_CHOOSE_NAME, attributes.get(CURRENT_STATE_KEY));
    }

    @Test
    public void testInterpretCreateChooseNameFirstCaps() {
        Input input = new Input();
        Map<String, Object> attributes = new HashMap<>();

        input.setInput("dani");
        attributes.put(CURRENT_STATE_KEY, DefaultLoginInterpreter.InterpreterState.CREATE_CHOOSE_NAME);

        Output result = interpreter.interpret(input, attributes);

        assertEquals("[red]Names must begin with an upper case letter.\n[default]Please choose a name: ", result.toString());
        assertFalse(result.getSecret());
        assertNull(attributes.get(TEMP_NAME_KEY));
        assertEquals(DefaultLoginInterpreter.InterpreterState.CREATE_CHOOSE_NAME, attributes.get(CURRENT_STATE_KEY));
    }

    @Test
    public void testInterpretCreateChooseNameOtherCaps() {
        Input input = new Input();
        Map<String, Object> attributes = new HashMap<>();

        input.setInput("DAni");
        attributes.put(CURRENT_STATE_KEY, DefaultLoginInterpreter.InterpreterState.CREATE_CHOOSE_NAME);

        Output result = interpreter.interpret(input, attributes);

        assertEquals("[red]Names must not contain upper case letters other than the first.\n[default]Please choose a name: ", result.toString());
        assertFalse(result.getSecret());
        assertNull(attributes.get(TEMP_NAME_KEY));
        assertEquals(DefaultLoginInterpreter.InterpreterState.CREATE_CHOOSE_NAME, attributes.get(CURRENT_STATE_KEY));
    }

    @Test
    public void testInterpretCreateChooseNameUserAlreadyExists() {
        Input input = new Input();
        Map<String, Object> attributes = new HashMap<>();

        input.setInput("Dani");
        attributes.put(CURRENT_STATE_KEY, DefaultLoginInterpreter.InterpreterState.CREATE_CHOOSE_NAME);

        when(userDetailsManager.userExists(eq("Dani"))).thenReturn(true);

        Output result = interpreter.interpret(input, attributes);

        assertEquals("[red]That name is already in use. Please try another!\n[default]Please choose a name: ", result.toString());
        assertFalse(result.getSecret());
        assertNull(attributes.get(TEMP_NAME_KEY));
        assertEquals(DefaultLoginInterpreter.InterpreterState.CREATE_CHOOSE_NAME, attributes.get(CURRENT_STATE_KEY));
    }

    @Test
    public void testInterpretCreateConfirmNameNo() {
        Input input = new Input();
        Map<String, Object> attributes = new HashMap<>();

        input.setInput("n");
        attributes.put(CURRENT_STATE_KEY, DefaultLoginInterpreter.InterpreterState.CREATE_CONFIRM_NAME);

        Output result = interpreter.interpret(input, attributes);

        assertEquals("[default]Please choose a name: ", result.toString());
        assertFalse(result.getSecret());
        assertEquals(DefaultLoginInterpreter.InterpreterState.CREATE_CHOOSE_NAME, attributes.get(CURRENT_STATE_KEY));
    }

    @Test
    public void testInterpretCreateConfirmNameYes() {
        Input input = new Input();
        Map<String, Object> attributes = new HashMap<>();

        input.setInput("y");
        attributes.put(CURRENT_STATE_KEY, DefaultLoginInterpreter.InterpreterState.CREATE_CONFIRM_NAME);

        Output result = interpreter.interpret(input, attributes);

        assertEquals("[default]Please choose a password: ", result.toString());
        assertTrue(result.getSecret());
        assertEquals(DefaultLoginInterpreter.InterpreterState.CREATE_CHOOSE_PASSWORD, attributes.get(CURRENT_STATE_KEY));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInterpretCreateChoosePassword() {
        Input input = new Input();
        Map<String, Object> attributes = new HashMap<>();

        input.setInput("Not!A_Real123Password");
        attributes.put(TEMP_NAME_KEY, "Dani");
        attributes.put(CURRENT_STATE_KEY, DefaultLoginInterpreter.InterpreterState.CREATE_CHOOSE_PASSWORD);
        attributes.put(HTTP_SESSION_ID_ATTR_NAME, UUID.randomUUID().toString());

        when(authenticationManager.authenticate(any(Authentication.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionRepository.findById(anyString())).thenReturn(session);

        Output result = interpreter.interpret(input, attributes);

        verify(userDetailsManager).createUser(any(User.class));
        verify(sessionRepository).findById(anyString());
        verify(session).setAttribute(eq(SPRING_SECURITY_CONTEXT_KEY), securityContextCaptor.capture());
        verify(sessionRepository).save(session);

        assertEquals("[default]Please confirm your password: ", result.toString());
        assertTrue(result.getSecret());
        assertEquals(DefaultLoginInterpreter.InterpreterState.CREATE_CONFIRM_PASSWORD, attributes.get(CURRENT_STATE_KEY));

        SecurityContext securityContext = securityContextCaptor.getValue();
        Authentication authentication = securityContext.getAuthentication();

        assertEquals("Dani", authentication.getPrincipal());
        assertEquals("Not!A_Real123Password", authentication.getCredentials());
    }

    @Test
    public void testInterpretCreateChoosePasswordTooShort() {
        Input input = new Input();
        Map<String, Object> attributes = new HashMap<>();

        input.setInput("Not!A_R");
        attributes.put(TEMP_NAME_KEY, "Dani");
        attributes.put(CURRENT_STATE_KEY, DefaultLoginInterpreter.InterpreterState.CREATE_CHOOSE_PASSWORD);
        attributes.put(HTTP_SESSION_ID_ATTR_NAME, UUID.randomUUID().toString());

        when(authenticationManager.authenticate(any(Authentication.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionRepository.findById(anyString())).thenReturn(session);

        Output result = interpreter.interpret(input, attributes);

        verifyZeroInteractions(sessionRepository, session);

        assertEquals("[red]Passwords must be at least 8 characters.\n[default]Please choose a password: ", result.toString());
        assertTrue(result.getSecret());
        assertEquals(DefaultLoginInterpreter.InterpreterState.CREATE_CHOOSE_PASSWORD, attributes.get(CURRENT_STATE_KEY));
    }

    @Test
    public void testInterpretCreateChoosePasswordSomethingBad() {
        Input input = new Input();
        Map<String, Object> attributes = new HashMap<>();

        input.setInput("Not!A_Real123Password");
        attributes.put(TEMP_NAME_KEY, "Dani");
        attributes.put(CURRENT_STATE_KEY, DefaultLoginInterpreter.InterpreterState.CREATE_CHOOSE_PASSWORD);
        attributes.put(HTTP_SESSION_ID_ATTR_NAME, UUID.randomUUID().toString());

        when(authenticationManager.authenticate(any(Authentication.class))).thenThrow(new BadCredentialsException("Boom!"));
        when(sessionRepository.findById(anyString())).thenReturn(session);

        Output result = interpreter.interpret(input, attributes);

        verifyZeroInteractions(sessionRepository, session);

        assertEquals("[red]Oops! Something bad happened. The error has been logged.\n[default]Please choose a password: ", result.toString());
        assertTrue(result.getSecret());
        assertEquals(DefaultLoginInterpreter.InterpreterState.CREATE_CHOOSE_PASSWORD, attributes.get(CURRENT_STATE_KEY));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInterpretCreateConfirmPassword() {
        Input input = new Input();
        Map<String, Object> attributes = new HashMap<>();

        input.setInput("Not!A_Real123Password");
        attributes.put(TEMP_NAME_KEY, "Dani");
        attributes.put(CURRENT_STATE_KEY, DefaultLoginInterpreter.InterpreterState.CREATE_CONFIRM_PASSWORD);
        attributes.put(HTTP_SESSION_ID_ATTR_NAME, UUID.randomUUID().toString());

        when(authenticationManager.authenticate(any(Authentication.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionRepository.findById(anyString())).thenReturn(session);

        Output result = interpreter.interpret(input, attributes);

        verify(sessionRepository).findById(anyString());
        verify(session).setAttribute(eq(SPRING_SECURITY_CONTEXT_KEY), securityContextCaptor.capture());
        verify(sessionRepository).save(session);

        assertEquals("[yellow]Welcome, Dani!\n[default]Dani> ", result.toString());
        assertFalse(result.getSecret());
        assertEquals(DefaultLoginInterpreter.InterpreterState.LOGGED_IN, attributes.get(CURRENT_STATE_KEY));

        SecurityContext securityContext = securityContextCaptor.getValue();
        Authentication authentication = securityContext.getAuthentication();

        assertEquals("Dani", authentication.getPrincipal());
        assertEquals("Not!A_Real123Password", authentication.getCredentials());
    }

    @Test
    public void testInterpretCreateConfirmPasswordTooShort() {
        Input input = new Input();
        Map<String, Object> attributes = new HashMap<>();

        input.setInput("Not!A_R");
        attributes.put(TEMP_NAME_KEY, "Dani");
        attributes.put(CURRENT_STATE_KEY, DefaultLoginInterpreter.InterpreterState.CREATE_CONFIRM_PASSWORD);
        attributes.put(HTTP_SESSION_ID_ATTR_NAME, UUID.randomUUID().toString());

        when(authenticationManager.authenticate(any(Authentication.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionRepository.findById(anyString())).thenReturn(session);

        Output result = interpreter.interpret(input, attributes);

        verifyZeroInteractions(sessionRepository, session);

        assertEquals("[red]Passwords must be at least 8 characters.\n[default]Please confirm your password: ", result.toString());
        assertTrue(result.getSecret());
        assertEquals(DefaultLoginInterpreter.InterpreterState.CREATE_CONFIRM_PASSWORD, attributes.get(CURRENT_STATE_KEY));
    }

    @Test
    public void testInterpretCreateConfirmPasswordSomethingBad() {
        Input input = new Input();
        Map<String, Object> attributes = new HashMap<>();

        input.setInput("Not!A_Real123Password");
        attributes.put(TEMP_NAME_KEY, "Dani");
        attributes.put(CURRENT_STATE_KEY, DefaultLoginInterpreter.InterpreterState.CREATE_CONFIRM_PASSWORD);
        attributes.put(HTTP_SESSION_ID_ATTR_NAME, UUID.randomUUID().toString());

        when(authenticationManager.authenticate(any(Authentication.class))).thenThrow(new BadCredentialsException("Boom!"));
        when(sessionRepository.findById(anyString())).thenReturn(session);

        Output result = interpreter.interpret(input, attributes);

        verifyZeroInteractions(sessionRepository, session);

        assertEquals("[red]Passwords do not match. Please try again!\n[default]Please choose a password: ", result.toString());
        assertTrue(result.getSecret());
        assertEquals(DefaultLoginInterpreter.InterpreterState.CREATE_CHOOSE_PASSWORD, attributes.get(CURRENT_STATE_KEY));
    }

    @Test
    public void testInterpretLoggedIn() {
        Input input = new Input();
        Map<String, Object> attributes = new HashMap<>();

        input.setInput("Hello world!");
        attributes.put(TEMP_NAME_KEY, "Dani");
        attributes.put(CURRENT_STATE_KEY, DefaultLoginInterpreter.InterpreterState.LOGGED_IN);

        Output result = interpreter.interpret(input, attributes);

        assertEquals("[cyan]Hello world!\n\n[default]Dani> ", result.toString());
    }
}
