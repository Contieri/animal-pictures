package com.example.animalpictures.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ImageTypesTest {

    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};

    @Test
    void trustsTheBytesOverAWrongHeader() {
        assertThat(ImageTypes.detect(PNG, MediaType.IMAGE_JPEG)).isEqualTo("image/png");
        assertThat(ImageTypes.detect(JPEG, MediaType.IMAGE_PNG)).isEqualTo("image/jpeg");
    }

    @Test
    void fallsBackToADeclaredImageType() {
        assertThat(ImageTypes.detect(new byte[] {1, 2, 3}, MediaType.parseMediaType("image/avif")))
                .isEqualTo("image/avif");
    }

    @Test
    void rejectsDataThatIsNotAnImage() {
        assertThat(ImageTypes.detect("<html>".getBytes(), MediaType.TEXT_HTML)).isNull();
        assertThat(ImageTypes.detect("<html>".getBytes(), null)).isNull();
    }
}
