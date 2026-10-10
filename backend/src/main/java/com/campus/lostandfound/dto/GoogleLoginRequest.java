package com.campus.lostandfound.dto;

import jakarta.validation.constraints.NotBlank;

/** The ID token (a signed JWT) that Google gives the frontend after "Sign in with Google". */
public record GoogleLoginRequest(
        @NotBlank(message = "Google ID token is required")
        String idToken) {
}
