package com.campus.lostandfound.model;

/**
 * Life cycle of an item report:
 * OPEN -> CLAIM_PENDING -> RETURNED, or CLOSED at any time by an admin/owner.
 *
 * An enum can hold data and methods. Here each constant carries an "active"
 * flag, so the rest of the code asks status.isActive() instead of repeating
 * a list of statuses everywhere.
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

    /** Active items are still shown in the public listing. */
    public boolean isActive() {
        return active;
    }
}
