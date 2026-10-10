package com.campus.lostandfound.dto;

import java.time.LocalDate;

import com.campus.lostandfound.model.ItemType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

/**
 * JSON the client sends to create a report. Only these fields can be set.
 * The reporter, status and creation time are NOT here, so a client cannot fake them.
 * (A Java record is a short way to write an immutable data class.)
 */
public record ItemCreateRequest(
        @NotBlank(message = "Item name is required")
        @Size(max = 100, message = "Item name must be at most 100 characters")
        String itemName,

        @NotBlank(message = "Description is required")
        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        @NotBlank(message = "Category is required")
        @Size(max = 50, message = "Category must be at most 50 characters")
        String category,

        @NotNull(message = "Type is required (LOST or FOUND)")
        ItemType type,

        @NotBlank(message = "Location is required")
        @Size(max = 100, message = "Location must be at most 100 characters")
        String location,

        @PastOrPresent(message = "Report date cannot be in the future")
        LocalDate reportDate,   // optional: today is used when missing

        @Size(max = 255, message = "Image path must be at most 255 characters")
        String imagePath) {     // optional
}
