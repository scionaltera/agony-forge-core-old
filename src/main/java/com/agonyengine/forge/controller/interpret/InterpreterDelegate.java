package com.agonyengine.forge.controller.interpret;

import com.agonyengine.forge.controller.Input;
import com.agonyengine.forge.controller.Output;
import com.agonyengine.forge.model.Connection;

public interface InterpreterDelegate {
    Output interpret(Interpreter primary, Input input, Connection connection);
    Output prompt(Interpreter primary, Connection connection);
}
