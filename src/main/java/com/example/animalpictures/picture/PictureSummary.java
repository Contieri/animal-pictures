package com.example.animalpictures.picture;

import java.time.Instant;

/** Picture metadata without the image bytes, so listing many pictures stays cheap. */
public record PictureSummary(
        Long id,
        AnimalType animalType,
        String contentType,
        long sizeBytes,
        String sourceUrl,
        Instant createdAt) {

    static PictureSummary of(Picture picture) {
        return new PictureSummary(picture.getId(), picture.getAnimalType(), picture.getContentType(),
                picture.getSizeBytes(), picture.getSourceUrl(), picture.getCreatedAt());
    }
}
