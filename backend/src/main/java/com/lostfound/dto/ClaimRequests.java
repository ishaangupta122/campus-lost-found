package com.lostfound.dto;

import com.lostfound.model.ClaimStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Claim request bodies. */
public final class ClaimRequests {

    private ClaimRequests() {
    }

    /** Why the item is theirs. */
    public record ClaimRequest(
            @NotBlank(message = "Message is required")
            @Size(max = 1000, message = "Message must be at most 1000 characters")
            String message) {
    }

    /** APPROVED or REJECTED. */
    public record ClaimDecisionRequest(
            @NotNull(message = "Decision is required (APPROVED or REJECTED)")
            ClaimStatus decision) {
    }
}
