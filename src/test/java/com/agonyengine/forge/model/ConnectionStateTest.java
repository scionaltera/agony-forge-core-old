package com.agonyengine.forge.model;

import org.junit.Test;

import static com.agonyengine.forge.model.ConnectionState.ASK_NEW;
import static org.junit.Assert.assertEquals;

public class ConnectionStateTest {
    @Test
    public void testIndex() {
        assertEquals(0, ASK_NEW.getIndex());
    }

    @Test
    public void testConverter() {
        new ConnectionState.Converter();
    }
}
