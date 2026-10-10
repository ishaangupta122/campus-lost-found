package com.campus.lostandfound.exception;

/** The request cannot be processed as sent (or a feature is not set up). HTTP 400. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
