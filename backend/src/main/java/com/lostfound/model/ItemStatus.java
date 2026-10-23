package com.lostfound.model;

/**
 * OPEN -> CLAIM_PENDING -> RETURNED, or CLOSED by the owner/admin.
 * Each constant carries an "active" flag (active items are shown in the public list).
 */
public enum ItemStatus {
    OPEN(true),
    CLAIM_PENDING(true),
    RETURNED(false),
    CLOSED(false);

    private final boolean active;

    ItemStatus(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }
}
