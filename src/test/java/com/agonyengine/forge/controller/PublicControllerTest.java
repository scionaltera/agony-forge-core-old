package com.agonyengine.forge.controller;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpSession;

import static org.junit.Assert.assertEquals;

public class PublicControllerTest {
    @Mock
    private HttpSession httpSession;

    private PublicController controller;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        controller = new PublicController();
    }

    @Test
    public void testPrivacyController() {
        assertEquals("privacy", controller.privacy());
    }

    @Test
    public void testPlayController() {
        assertEquals("play", controller.play(httpSession));
    }
}
