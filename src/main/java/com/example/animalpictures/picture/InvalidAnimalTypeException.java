package com.example.animalpictures.picture;

public class InvalidAnimalTypeException extends RuntimeException {

    public InvalidAnimalTypeException(String value) {
        super("Unknown animal type '" + value + "'. Supported values: " + AnimalType.supportedValues());
    }
}
