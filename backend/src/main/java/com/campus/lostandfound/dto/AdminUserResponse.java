package com.campus.lostandfound.dto;

import java.time.LocalDateTime;

import com.campus.lostandfound.model.Role;
import com.campus.lostandfound.model.User;

/** One row of the admin's user list. The password hash is never included. */
public record AdminUserResponse(
        Long id,
        String name,
        String email,
        Role role,
        boolean googleAccount,
        LocalDateTime createdAt) {

    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(),
                user.getGoogleId() != null, user.getCreatedAt());
    }
}
