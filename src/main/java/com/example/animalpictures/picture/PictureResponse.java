package com.example.animalpictures.picture;

import java.time.Instant;

/** Metadata returned by the API. The image itself is served by {@code imageUrl}. */
public record PictureResponse(
        long id,
        String animal,
        String contentType,
        long sizeBytes,
        String sourceUrl,
        Instant createdAt,
        String imageUrl) {

    static PictureResponse from(Picture picture) {
        return new PictureResponse(
                picture.getId(),
                picture.getAnimalType().key(),
                picture.getContentType(),
                picture.getSizeBytes(),
                picture.getSourceUrl(),
                picture.getCreatedAt(),
                "/api/pictures/" + picture.getId() + "/image");
    }
}
