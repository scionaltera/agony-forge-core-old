package com.agonyengine.forge.controller;

import com.agonyengine.forge.controller.greeting.ClasspathGreetingLoader;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WebSocketControllerTest {
    private WebSocketController controller;

    @Before
    public void setUp() {
        ClasspathGreetingLoader loader = new ClasspathGreetingLoader();

        controller = new WebSocketController(loader);
    }

    @Test
    public void testOnSubscribe() {
        Output o1 = new Output(
            "[yellow]Hello&nbsp;world!",
            "[yellow]Hello world!",
            "[yellow]Type something!",
            "[default]> ");
        Output o2 = controller.onSubscribe();

        assertEquals(o1, o2);
    }

    @Test
    public void testOnInput() {
        Input input = new Input();

        input.setInput("Testing");

        assertEquals(new Output("[cyan]" + input, "", "[default]> "), controller.onInput(input));
    }
}
