package com.campus.lostandfound.dto;

import com.campus.lostandfound.model.ClaimStatus;

import jakarta.validation.constraints.NotNull;

/** The admin's decision on a claim: APPROVED or REJECTED. */
public record ClaimDecisionRequest(
        @NotNull(message = "Decision is required (APPROVED or REJECTED)")
        ClaimStatus decision) {
}
