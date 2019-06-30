package com.agonyengine.forge.model;

import org.junit.Test;

import static com.agonyengine.forge.model.PrimaryConnectionState.LOGIN;
import static org.junit.Assert.assertEquals;

public class PrimaryConnectionStateTest {
    @Test
    public void testIndex() {
        assertEquals(0, LOGIN.getIndex());
    }

    @Test
    public void testConverter() {
        new PrimaryConnectionState.Converter();
    }
}
