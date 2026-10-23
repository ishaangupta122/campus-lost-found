package com.lostfound.dto;

/** Admin dashboard counts. */
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
