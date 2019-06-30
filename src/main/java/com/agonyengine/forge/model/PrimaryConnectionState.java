package com.agonyengine.forge.model;

import com.agonyengine.forge.model.util.BaseEnumSetConverter;
import com.agonyengine.forge.model.util.PersistentEnum;

public enum PrimaryConnectionState implements PersistentEnum {
    LOGIN(0),
    CREATION(1),
    IN_GAME(2),
    MENU(3);

    private int index;

    PrimaryConnectionState(int index) {
        this.index = index;
    }

    @Override
    public int getIndex() {
        return index;
    }

    public static class Converter extends BaseEnumSetConverter<PrimaryConnectionState> {
        public Converter() {
            super(PrimaryConnectionState.class);
        }
    }
}
