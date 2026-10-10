package com.campus.lostandfound.dto;

import java.time.LocalDateTime;

import com.campus.lostandfound.model.Claim;
import com.campus.lostandfound.model.ClaimStatus;
import com.campus.lostandfound.model.ItemStatus;

/** A claim as shown to the frontend: ids and names, never entities or emails/hashes. */
public record ClaimResponse(
        Long id,
        Long itemId,
        String itemName,
        ItemStatus itemStatus,
        Long claimantId,
        String claimantName,
        String message,
        ClaimStatus status,
        LocalDateTime createdAt) {

    public static ClaimResponse from(Claim claim) {
        return new ClaimResponse(
                claim.getId(),
                claim.getItem().getId(),
                claim.getItem().getItemName(),
                claim.getItem().getStatus(),
                claim.getClaimant().getId(),
                claim.getClaimant().getName(),
                claim.getMessage(),
                claim.getStatus(),
                claim.getCreatedAt());
    }
}
