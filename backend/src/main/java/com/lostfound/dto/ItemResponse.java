package com.lostfound.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.lostfound.model.Item;
import com.lostfound.model.ItemStatus;
import com.lostfound.model.ItemType;

/** Report as sent to the client (reporter id and name only). */
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
