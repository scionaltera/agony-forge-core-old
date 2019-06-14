package com.agonyengine.forge.controller.greeting;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Collections;
import java.util.List;

@Component
public class FileGreetingLoader extends GreetingLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileGreetingLoader.class);
    private static final String GREETING_EXTERNAL = "./config/greeting.txt";

    private String testFilename = null;

    public FileGreetingLoader() {

    }

    FileGreetingLoader(String filename) {
        this.testFilename = filename;
    }

    @Override
    public List<String> load() {
        File external = new File(testFilename != null ? testFilename : GREETING_EXTERNAL);

        try {
            BufferedReader reader = IOUtils.buffer(new FileReader(external));
            LOGGER.info("Loaded external greeting: {}", external.getAbsolutePath());

            return parse(reader);
        } catch (FileNotFoundException e) {
            LOGGER.warn("Unable to read external greeting: {}", GREETING_EXTERNAL);
        }

        return Collections.emptyList();
    }
}
