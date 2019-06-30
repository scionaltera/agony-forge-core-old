package com.agonyengine.forge.controller.interpret;

import com.agonyengine.forge.controller.Input;
import com.agonyengine.forge.controller.Output;
import com.agonyengine.forge.model.Connection;

public abstract class BaseInterpreterDelegate implements InterpreterDelegate {
    @Override
    public abstract Output interpret(Interpreter primary, Input input, Connection connection);

    @Override
    public abstract Output prompt(Interpreter primary, Connection connection);
}
