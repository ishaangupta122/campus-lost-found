package com.lostfound.controller;

import org.springframework.security.core.Authentication;

/** Logged-in user's id (the JWT subject). */
final class CurrentUser {

    private CurrentUser() {
    }

    static Long id(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }
}
