package com.campus.lostandfound.dto;

import com.campus.lostandfound.model.Role;
import com.campus.lostandfound.model.User;

/** What the API reveals about a user. The password hash is never included. */
public record UserResponse(Long id, String name, String email, Role role) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}
