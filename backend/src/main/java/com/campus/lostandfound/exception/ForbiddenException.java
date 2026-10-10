package com.campus.lostandfound.exception;

/** The user is known but not allowed to do this (e.g. editing someone else's report). HTTP 403. */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
