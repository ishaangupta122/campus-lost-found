package com.lostfound.dto;

import java.time.LocalDateTime;

import com.lostfound.model.Claim;
import com.lostfound.model.ClaimStatus;
import com.lostfound.model.ItemStatus;

/** Claim as shown to the frontend (ids and names only). */
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
