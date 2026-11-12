package com.example.animalpictures.picture;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public enum AnimalType {
    CAT,
    DOG,
    BEAR;

    /** Parses "cat", "Dog", "BEAR"... ignoring case. */
    public static AnimalType from(String value) {
        if (value != null) {
            for (AnimalType type : values()) {
                if (type.name().equalsIgnoreCase(value.trim())) {
                    return type;
                }
            }
        }
        throw new InvalidAnimalTypeException(value);
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static String supportedValues() {
        return Arrays.stream(values()).map(AnimalType::key).collect(Collectors.joining(", "));
    }
}
