package com.example.animalpictures.client;

/** A picture downloaded from a source API, not yet stored. */
public record FetchedPicture(byte[] data, String contentType, String sourceUrl) {
}
