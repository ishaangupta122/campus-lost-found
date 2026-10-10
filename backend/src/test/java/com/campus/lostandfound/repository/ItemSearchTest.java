package com.campus.lostandfound.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Sort;

import com.campus.lostandfound.model.Item;
import com.campus.lostandfound.model.ItemStatus;
import com.campus.lostandfound.model.ItemType;
import com.campus.lostandfound.model.Role;
import com.campus.lostandfound.model.User;

/**
 * Checks the search/filter query against real PostgreSQL. A random token makes
 * our test rows unique, so existing data in your database cannot disturb the
 * result. Everything is rolled back at the end.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ItemSearchTest {

    private static final List<ItemStatus> ACTIVE = List.of(ItemStatus.OPEN, ItemStatus.CLAIM_PENDING);

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserRepository userRepository;

    private String token;

    @BeforeEach
    void saveSampleItems() {
        token = UUID.randomUUID().toString().replace("-", "");
        User reporter = userRepository.save(
                new User("Tester", "tester-" + token + "@campus.edu", "hash", Role.STUDENT));

        // A: found, library, bags, open - token in the NAME
        itemRepository.save(new Item("Backpack " + token, "Blue", "Bags", ItemType.FOUND,
                "Main Library", LocalDate.now(), reporter));
        // B: lost, canteen - token in the DESCRIPTION
        itemRepository.save(new Item("Umbrella", "Black, tag " + token, "Accessories", ItemType.LOST,
                "Canteen", LocalDate.now(), reporter));
        // C: found, library, closed - token in the name
        Item closed = new Item("Wallet " + token, "Brown", "Bags", ItemType.FOUND,
                "Main Library", LocalDate.now(), reporter);
        closed.setStatus(ItemStatus.CLOSED);
        itemRepository.save(closed);
    }

    private List<Item> search(String keyword, ItemType type, String category, String location,
                              List<ItemStatus> statuses) {
        return itemRepository.findAll(
                ItemSpecifications.search(keyword, type, category, location, statuses),
                Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Test
    void keywordSearchesNameAndDescriptionOfActiveItems() {
        assertEquals(2, search(token.toUpperCase(), null, null, null, ACTIVE).size());
    }

    @Test
    void typeFilterNarrowsResult() {
        assertEquals(1, search(token, ItemType.FOUND, null, null, ACTIVE).size());
        assertEquals(1, search(token, ItemType.LOST, null, null, ACTIVE).size());
    }

    @Test
    void categoryIsCaseInsensitiveAndLocationIsPartialMatch() {
        assertEquals(1, search(token, null, "BAGS", "library", ACTIVE).size());
    }

    @Test
    void statusFilterFindsClosedItems() {
        List<Item> closed = search(token, null, null, null, List.of(ItemStatus.CLOSED));

        assertEquals(1, closed.size());
        assertEquals(ItemStatus.CLOSED, closed.get(0).getStatus());
    }

    @Test
    void blankFiltersAreIgnored() {
        assertEquals(2, search(token, null, " ", "", ACTIVE).size());
    }
}
