package com.campus.lostandfound.dto;

/** Numbers shown on the admin dashboard. */
public record DashboardResponse(
        long totalUsers,
        long totalLostItems,
        long totalFoundItems,
        long openItems,
        long claimPendingItems,
        long returnedItems,
        long closedItems,
        long pendingClaims) {
}
