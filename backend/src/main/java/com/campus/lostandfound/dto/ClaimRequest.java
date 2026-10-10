package com.campus.lostandfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** What a student sends to claim an item: a message explaining why it is theirs. */
public record ClaimRequest(
        @NotBlank(message = "Message is required")
        @Size(max = 1000, message = "Message must be at most 1000 characters")
        String message) {
}
