package com.example.animalpictures.client;

/** A source API could not deliver a picture (network error, bad status, not an image). */
public class PictureFetchException extends RuntimeException {

    public PictureFetchException(String message) {
        super(message);
    }

    public PictureFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}
