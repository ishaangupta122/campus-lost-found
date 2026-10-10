package com.campus.lostandfound.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

/**
 * JSON for editing a report. It has no type, status or reporter field on purpose:
 * those protected fields cannot be changed through this endpoint.
 */
public record ItemUpdateRequest(
        @NotBlank(message = "Item name is required")
        @Size(max = 100, message = "Item name must be at most 100 characters")
        String itemName,

        @NotBlank(message = "Description is required")
        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        @NotBlank(message = "Category is required")
        @Size(max = 50, message = "Category must be at most 50 characters")
        String category,

        @NotBlank(message = "Location is required")
        @Size(max = 100, message = "Location must be at most 100 characters")
        String location,

        @PastOrPresent(message = "Report date cannot be in the future")
        LocalDate reportDate,   // optional: unchanged when missing

        @Size(max = 255, message = "Image path must be at most 255 characters")
        String imagePath) {
}
