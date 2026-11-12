package com.example.animalpictures.picture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AnimalTypeTest {

    @ParameterizedTest
    @ValueSource(strings = {"dog", "Dog", "DOG", " dog "})
    void parsesIgnoringCaseAndSpaces(String value) {
        assertThat(AnimalType.from(value)).isEqualTo(AnimalType.DOG);
    }

    @Test
    void rejectsUnknownAnimal() {
        assertThatThrownBy(() -> AnimalType.from("cow"))
                .isInstanceOf(InvalidAnimalTypeException.class)
                .hasMessageContaining("cow")
                .hasMessageContaining("cat, dog, bear");
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> AnimalType.from(null)).isInstanceOf(InvalidAnimalTypeException.class);
    }
}
