package com.lostfound.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Request and response bodies of the /api/auth endpoints. */
public final class AuthPayloads {

    private AuthPayloads() {
    }

    public record LoginRequest(
            @NotBlank(message = "Email is required")
            String email,

            @NotBlank(message = "Password is required")
            String password) {
    }

    /** No role field: everyone who registers is a STUDENT. */
    public record RegisterRequest(
            @NotBlank(message = "Name is required")
            @Size(max = 100, message = "Name must be at most 100 characters")
            String name,

            @NotBlank(message = "Email is required")
            @Email(message = "Email must be a valid address")
            @Pattern(regexp = "(?i)^\\s*[^@\\s]+@thapar\\.edu\\s*$",
                     message = "Use your @thapar.edu email address")
            @Size(max = 150, message = "Email must be at most 150 characters")
            String email,

            @NotBlank(message = "Password is required")
            @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters")
            String password) {
    }

    /** Google ID token sent by the frontend. */
    public record GoogleLoginRequest(
            @NotBlank(message = "Google ID token is required")
            String idToken) {
    }

    /** Token plus user, returned by register and login. */
    public record AuthResponse(String token, String tokenType, long expiresInSeconds, UserResponse user) {
    }
}
