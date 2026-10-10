package com.campus.lostandfound.dto;

/** Returned by register and login: the token to send on later requests, plus the user. */
public record AuthResponse(String token, String tokenType, long expiresInSeconds, UserResponse user) {
}
