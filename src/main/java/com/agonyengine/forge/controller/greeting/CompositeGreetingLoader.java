package com.agonyengine.forge.controller.greeting;

import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

@Component
public class CompositeGreetingLoader extends GreetingLoader {
    private FileGreetingLoader fileGreetingLoader;
    private ClasspathGreetingLoader classpathGreetingLoader;

    @Inject
    public CompositeGreetingLoader(
        FileGreetingLoader fileGreetingLoader,
        ClasspathGreetingLoader classpathGreetingLoader) {

        this.fileGreetingLoader = fileGreetingLoader;
        this.classpathGreetingLoader = classpathGreetingLoader;
    }

    @Override
    public List<String> load() {
        return Optional
            .of(fileGreetingLoader.load())
            .filter(greeting -> !greeting.isEmpty())
            .orElseGet(() -> classpathGreetingLoader.load());
    }
}
