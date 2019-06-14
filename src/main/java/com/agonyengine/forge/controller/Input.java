package com.agonyengine.forge.controller;

import java.util.Objects;

public class Input {
    private String input;

    public void setInput(String input) {
        this.input = input;
    }

    public String getInput() {
        return input;
    }

    @Override
    public String toString() {
        return input;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Input)) return false;
        Input input1 = (Input) o;
        return Objects.equals(input, input1.input);
    }

    @Override
    public int hashCode() {
        return Objects.hash(input);
    }
}
