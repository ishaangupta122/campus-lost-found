package com.campus.lostandfound.controller;

import org.springframework.security.core.Authentication;

/** Reads the logged-in user's id from the verified token (its "subject"). */
final class CurrentUser {

    private CurrentUser() {
    }

    static Long id(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }
}
