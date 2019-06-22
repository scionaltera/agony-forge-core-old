package com.agonyengine.forge.controller.interpret;

import com.agonyengine.forge.controller.Input;
import com.agonyengine.forge.controller.Output;
import com.agonyengine.forge.model.Connection;
import com.agonyengine.forge.model.Creature;

public interface Interpreter {
    Output interpret(Input input, Connection connection);
    Output prompt(Connection connection);
    void echo(Creature target, Output message);
    void echoToWorld(Output message, Creature ... exclude);
}
