package com.agonyengine.forge.controller.interpret;

import com.agonyengine.forge.controller.Input;
import com.agonyengine.forge.controller.Output;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class EchoInterpreter implements Interpreter {
    @Override
    public Output interpret(Input input, Map<String, Object> attributes) {
        return new Output("[cyan]" + input).append(prompt(attributes));
    }

    @Override
    public Output prompt(Map<String, Object> attributes) {
        return new Output("[default]> ");
    }
}
