package com.campus.lostandfound.exception;

/** We could not tell who the caller is. HTTP 401. */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
