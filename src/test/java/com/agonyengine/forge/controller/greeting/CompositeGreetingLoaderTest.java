package com.agonyengine.forge.controller.greeting;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class CompositeGreetingLoaderTest {
    @Test
    public void testLoadFromFile() {
        FileGreetingLoader fileLoader = spy(new FileGreetingLoader("./src/test/resources/greeting.txt"));
        ClasspathGreetingLoader classpathLoader = spy(new ClasspathGreetingLoader());

        CompositeGreetingLoader loader = new CompositeGreetingLoader(fileLoader, classpathLoader);

        List<String> greeting = loader.load();

        assertEquals("[yellow]Hello&nbsp;world!", greeting.get(0));
        assertEquals("[yellow]Hello world!", greeting.get(1));

        verify(fileLoader).load();
        verify(classpathLoader, never()).load();
    }

    @Test
    public void testLoadFromClasspath() {
        FileGreetingLoader fileLoader = spy(new FileGreetingLoader());
        ClasspathGreetingLoader classpathLoader = spy(new ClasspathGreetingLoader());

        CompositeGreetingLoader loader = new CompositeGreetingLoader(fileLoader, classpathLoader);

        List<String> greeting = loader.load();

        assertEquals("[yellow]Hello&nbsp;world!", greeting.get(0));
        assertEquals("[yellow]Hello world!", greeting.get(1));

        verify(fileLoader).load();
        verify(classpathLoader).load();
    }
}
