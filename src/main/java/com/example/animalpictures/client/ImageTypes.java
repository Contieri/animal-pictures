package com.example.animalpictures.client;

import org.springframework.http.MediaType;

/**
 * Detects the real image format from the first bytes ("magic numbers").
 * Some sources send a wrong Content-Type (place.dog serves PNG files labelled image/jpeg).
 */
final class ImageTypes {

    private ImageTypes() {
    }

    /** @return the image media type, or {@code null} when the data is not an image */
    static String detect(byte[] data, MediaType declared) {
        if (startsWith(data, 0xFF, 0xD8, 0xFF)) {
            return MediaType.IMAGE_JPEG_VALUE;
        }
        if (startsWith(data, 0x89, 'P', 'N', 'G')) {
            return MediaType.IMAGE_PNG_VALUE;
        }
        if (startsWith(data, 'G', 'I', 'F', '8')) {
            return MediaType.IMAGE_GIF_VALUE;
        }
        if (data.length >= 12 && startsWith(data, 'R', 'I', 'F', 'F')
                && data[8] == 'W' && data[9] == 'E' && data[10] == 'B' && data[11] == 'P') {
            return "image/webp";
        }
        if (declared != null && "image".equalsIgnoreCase(declared.getType())) {
            return declared.getType() + "/" + declared.getSubtype();
        }
        return null;
    }

    private static boolean startsWith(byte[] data, int... prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if ((data[i] & 0xFF) != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
