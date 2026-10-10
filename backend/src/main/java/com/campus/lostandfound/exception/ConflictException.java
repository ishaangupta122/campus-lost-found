package com.campus.lostandfound.exception;

/** The request is valid but clashes with the current state (e.g. item already closed). HTTP 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
