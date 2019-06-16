package com.agonyengine.forge.controller.interpret;

import com.agonyengine.forge.controller.Input;
import com.agonyengine.forge.controller.Output;

import java.util.Map;

public interface Interpreter {
    Output interpret(Input input, Map<String, Object> attributes);
    Output prompt(Map<String, Object> attributes);
}
