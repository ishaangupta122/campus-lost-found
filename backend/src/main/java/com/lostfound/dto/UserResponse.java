package com.lostfound.dto;

import java.time.LocalDateTime;

import com.lostfound.model.Role;
import com.lostfound.model.User;

/** User data shown by the API (never the password hash). */
public record UserResponse(
        Long id,
        String name,
        String email,
        Role role,
        boolean googleAccount,
        LocalDateTime createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(),
                user.getGoogleId() != null, user.getCreatedAt());
    }
}
