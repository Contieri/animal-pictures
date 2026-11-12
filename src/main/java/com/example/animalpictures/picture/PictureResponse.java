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
        return from(PictureSummary.of(picture));
    }

    static PictureResponse from(PictureSummary summary) {
        return new PictureResponse(
                summary.id(),
                summary.animalType().key(),
                summary.contentType(),
                summary.sizeBytes(),
                summary.sourceUrl(),
                summary.createdAt(),
                "/api/pictures/" + summary.id() + "/image");
    }
}
