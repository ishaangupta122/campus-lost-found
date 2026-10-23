package com.lostfound.dto;

import java.time.LocalDate;

import com.lostfound.model.ItemType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

/**
 * Body for creating or editing a report. No reporter or status field, so clients cannot set them.
 * "type" is used only on create.
 */
public record ItemRequest(
        @NotBlank(message = "Item name is required")
        @Size(max = 100, message = "Item name must be at most 100 characters")
        String itemName,

        @NotBlank(message = "Description is required")
        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        @NotBlank(message = "Category is required")
        @Size(max = 50, message = "Category must be at most 50 characters")
        String category,

        ItemType type,          // required on create, ignored on edit

        @NotBlank(message = "Location is required")
        @Size(max = 100, message = "Location must be at most 100 characters")
        String location,

        @PastOrPresent(message = "Report date cannot be in the future")
        LocalDate reportDate,   // optional (defaults to today)

        @Size(max = 255, message = "Image path must be at most 255 characters")
        String imagePath) {
}
