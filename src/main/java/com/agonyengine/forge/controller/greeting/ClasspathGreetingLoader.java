package com.agonyengine.forge.controller.greeting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

@Component
public class ClasspathGreetingLoader extends GreetingLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClasspathGreetingLoader.class);
    private static final String GREETING_FILENAME = "/greeting.txt";

    @Override
    public List<String> load() {
        InputStream is = GreetingLoader.class.getResourceAsStream(GREETING_FILENAME);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        LOGGER.info("Loaded embedded greeting: {}", GREETING_FILENAME);

        return parse(reader);
    }
}
