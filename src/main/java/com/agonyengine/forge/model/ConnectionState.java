package com.agonyengine.forge.model;

import com.agonyengine.forge.model.util.BaseEnumSetConverter;
import com.agonyengine.forge.model.util.PersistentEnum;

public enum ConnectionState implements PersistentEnum {
    ASK_NEW(0),
    LOGIN_ASK_NAME(1),
    LOGIN_ASK_PASSWORD(2),
    CREATE_CHOOSE_NAME(3),
    CREATE_CONFIRM_NAME(4),
    CREATE_CHOOSE_PASSWORD(5),
    CREATE_CONFIRM_PASSWORD(6),
    IN_GAME(7);

    private int index;

    ConnectionState(int index) {
        this.index = index;
    }

    @Override
    public int getIndex() {
        return index;
    }

    public static class Converter extends BaseEnumSetConverter<ConnectionState> {
        public Converter() {
            super(ConnectionState.class);
        }
    }
}
