package com.campus.lostandfound.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import com.campus.lostandfound.model.Claim;
import com.campus.lostandfound.model.ClaimStatus;
import com.campus.lostandfound.model.Item;
import com.campus.lostandfound.model.ItemStatus;
import com.campus.lostandfound.model.ItemType;
import com.campus.lostandfound.model.Role;
import com.campus.lostandfound.model.User;

/**
 * Runs against the real PostgreSQL database (settings from .env).
 * Replace.NONE stops Spring from swapping in an in-memory database.
 * @DataJpaTest wraps each test in a transaction that is rolled back at the
 * end, so no test data stays in your database.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ClaimRepository claimRepository;

    private User newUser(String name) {
        String email = name.toLowerCase() + "-" + UUID.randomUUID() + "@campus.edu";
        return userRepository.save(new User(name, email, "hash", Role.STUDENT));
    }

    @Test
    void savesItemAndClaimWithRelationships() {
        User reporter = newUser("Asha");
        User claimant = newUser("Ravi");
        Item item = itemRepository.save(new Item("Blue Backpack", "Has a keychain", "Bags",
                ItemType.FOUND, "Library", LocalDate.now(), reporter));
        claimRepository.save(new Claim(item, claimant, "It is mine, it has my name inside"));

        assertNotNull(item.getId());
        assertNotNull(item.getCreatedAt());
        assertEquals(ItemStatus.OPEN, item.getStatus());

        List<Item> reporterItems = itemRepository.findByReporterIdOrderByCreatedAtDesc(reporter.getId());
        assertEquals(1, reporterItems.size());

        assertTrue(claimRepository.existsByItemIdAndClaimantIdAndStatus(
                item.getId(), claimant.getId(), ClaimStatus.PENDING));
        assertFalse(claimRepository.existsByItemIdAndStatus(item.getId(), ClaimStatus.APPROVED));
        assertEquals(1, claimRepository.findByClaimantIdOrderByCreatedAtDesc(claimant.getId()).size());
    }

    @Test
    void findsUserByEmail() {
        User user = newUser("Meera");

        assertTrue(userRepository.existsByEmail(user.getEmail()));
        assertEquals(user.getId(), userRepository.findByEmail(user.getEmail()).orElseThrow().getId());
    }

    @Test
    void rejectsDuplicateEmail() {
        User first = newUser("Kiran");

        assertThrows(DataIntegrityViolationException.class, () ->
                userRepository.saveAndFlush(new User("Copy", first.getEmail(), "hash", Role.STUDENT)));
    }

    @Test
    void findsPendingClaimsOfAnItemAndCanLockTheItemRow() {
        User reporter = newUser("Dev");
        User claimant = newUser("Ila");
        Item item = itemRepository.save(new Item("Keys", "Two keys", "Keys",
                ItemType.FOUND, "Canteen", LocalDate.now(), reporter));
        claimRepository.save(new Claim(item, claimant, "mine"));

        assertEquals(1, claimRepository.findByItemIdAndStatus(item.getId(), ClaimStatus.PENDING).size());
        assertTrue(claimRepository.existsByItemId(item.getId()));
        assertTrue(itemRepository.findByIdForUpdate(item.getId()).isPresent());
    }
}
