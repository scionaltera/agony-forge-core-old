package com.agonyengine.forge.controller.greeting;

import java.io.BufferedReader;
import java.util.List;
import java.util.stream.Collectors;

public abstract class GreetingLoader {
    public abstract List<String> load();

    static List<String> parse(BufferedReader reader) {
        return reader.lines()
            .map(line -> {
                if (line.startsWith("#")) {
                    return "";
                } else if (line.startsWith("*")) {
                    return line.substring(1);
                } else {
                    return line.replace(" ", "&nbsp;");
                }
            })
            .filter(line -> line.length() > 0)
            .collect(Collectors.toList());
    }
}
