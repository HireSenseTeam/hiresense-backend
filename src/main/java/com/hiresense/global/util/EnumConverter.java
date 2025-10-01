package com.hiresense.global.util;

import com.hiresense.global.error.exception.InvalidInputValueException;

public class EnumConverter {

    public static <T extends Enum<T>> T safeValueOf(Class<T> enumType, String name) {
        if (name == null) {
            return null;
        }
        try {
            return Enum.valueOf(enumType, name.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidInputValueException();
        }
    }
}
