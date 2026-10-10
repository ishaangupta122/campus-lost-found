package com.campus.lostandfound.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/** Plain unit tests: no Spring and no database needed. */
class ItemTest {

    private final User reporter = new User("Asha", "asha@campus.edu", "hash", Role.STUDENT);

    @Test
    void newItemStartsOpenAndActive() {
        Item item = new Item("Backpack", "Blue", "Bags", ItemType.LOST, "Library",
                LocalDate.of(2026, 10, 1), reporter);

        assertEquals(ItemStatus.OPEN, item.getStatus());
        assertTrue(item.isActive());
        assertEquals(LocalDate.of(2026, 10, 1), item.getReportDate());
    }

    @Test
    void shorterConstructorUsesTodayAsReportDate() {
        Item item = new Item("Keys", "Two keys", "Keys", ItemType.FOUND, "Canteen", reporter);

        assertEquals(LocalDate.now(), item.getReportDate());
    }

    @Test
    void returnedAndClosedItemsAreNotActive() {
        Item item = new Item("Keys", "Two keys", "Keys", ItemType.FOUND, "Canteen", reporter);

        item.setStatus(ItemStatus.RETURNED);
        assertFalse(item.isActive());

        item.setStatus(ItemStatus.CLOSED);
        assertFalse(item.isActive());
    }

    @Test
    void newClaimStartsPending() {
        Item item = new Item("Keys", "Two keys", "Keys", ItemType.FOUND, "Canteen", reporter);
        Claim claim = new Claim(item, reporter, "These are mine");

        assertEquals(ClaimStatus.PENDING, claim.getStatus());
    }
}
