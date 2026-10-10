package com.campus.lostandfound.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.campus.lostandfound.model.Item;
import com.campus.lostandfound.model.ItemStatus;
import com.campus.lostandfound.model.ItemType;

/**
 * JSON sent back to the client. It exposes only the reporter's id and name,
 * never the reporter's email or password hash, and not the raw entity.
 */
public record ItemResponse(
        Long id,
        String itemName,
        String description,
        String category,
        ItemType type,
        String location,
        LocalDate reportDate,
        ItemStatus status,
        String imagePath,
        Long reporterId,
        String reporterName,
        LocalDateTime createdAt) {

    /** Converts a database entity into a safe response object. */
    public static ItemResponse from(Item item) {
        return new ItemResponse(
                item.getId(),
                item.getItemName(),
                item.getDescription(),
                item.getCategory(),
                item.getType(),
                item.getLocation(),
                item.getReportDate(),
                item.getStatus(),
                item.getImagePath(),
                item.getReporter().getId(),
                item.getReporter().getName(),
                item.getCreatedAt());
    }
}
