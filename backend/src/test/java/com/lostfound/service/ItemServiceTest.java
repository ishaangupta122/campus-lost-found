package com.lostfound.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import com.lostfound.dto.ItemRequest;
import com.lostfound.dto.ItemResponse;
import com.lostfound.exception.BadRequestException;
import com.lostfound.exception.ConflictException;
import com.lostfound.model.Claim;
import com.lostfound.model.ClaimStatus;
import com.lostfound.model.Item;
import com.lostfound.model.ItemStatus;
import com.lostfound.model.ItemType;
import com.lostfound.model.Role;
import com.lostfound.model.User;
import com.lostfound.repository.ClaimRepository;
import com.lostfound.repository.ItemRepository;
import com.lostfound.repository.UserRepository;

/** Unit tests with fake (mock) repositories: no database, no Spring. */
class ItemServiceTest {

    private ItemRepository itemRepository;
    private UserRepository userRepository;
    private ClaimRepository claimRepository;
    private ImageStorageService imageStorageService;
    private ItemService itemService;

    private User owner;
    private User otherStudent;
    private Item item;

    @BeforeEach
    void setUp() {
        itemRepository = mock(ItemRepository.class);
        userRepository = mock(UserRepository.class);
        claimRepository = mock(ClaimRepository.class);
        imageStorageService = mock(ImageStorageService.class);
        itemService = new ItemService(itemRepository, userRepository, claimRepository, imageStorageService);

        owner = fakeUser(1L, "Alice", Role.STUDENT);
        otherStudent = fakeUser(2L, "Bob", Role.STUDENT);
        fakeUser(3L, "Admin", Role.ADMIN);

        item = new Item("Backpack", "Blue", "Bags", ItemType.LOST, "Library", LocalDate.now(), owner);
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(itemRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(item));
        when(itemRepository.save(any(Item.class))).thenAnswer(call -> call.getArgument(0));
    }

    private User fakeUser(Long id, String name, Role role) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getName()).thenReturn(name);
        when(user.getRole()).thenReturn(role);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        return user;
    }

    private ItemRequest updateRequest() {
        return new ItemRequest("Red Backpack", "Now red", "Bags", null, "Canteen", null, null);
    }

    @Test
    void studentCanCreateItemAndItStartsOpen() {
        ItemRequest request = new ItemRequest(" Keys ", "Two keys", "Keys",
                ItemType.FOUND, "Canteen", null, null);

        ItemResponse response = itemService.createItem(1L, request);

        assertEquals("Keys", response.itemName());
        assertEquals(ItemStatus.OPEN, response.status());
        assertEquals(LocalDate.now(), response.reportDate());
        assertEquals("Alice", response.reporterName());
    }

    @Test
    void adminCannotCreateItem() {
        ItemRequest request = new ItemRequest("Keys", "Two keys", "Keys",
                ItemType.FOUND, "Canteen", null, null);

        assertThrows(AccessDeniedException.class, () -> itemService.createItem(3L, request));
        verify(itemRepository, never()).save(any(Item.class));
    }

    @Test
    void createWithoutTypeIsRejected() {
        ItemRequest request = new ItemRequest("Keys", "Two keys", "Keys", null, "Canteen", null, null);

        assertThrows(BadRequestException.class, () -> itemService.createItem(1L, request));
        verify(itemRepository, never()).save(any(Item.class));
    }

    @Test
    void unknownUserIsUnauthorized() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> itemService.getMyItems(99L));
    }

    @Test
    void ownerCanUpdateOwnItem() {
        ItemResponse response = itemService.updateItem(1L, 10L, updateRequest());

        assertEquals("Red Backpack", response.itemName());
        assertEquals("Canteen", response.location());
        assertEquals(ItemType.LOST, response.type());
    }

    @Test
    void otherStudentCannotUpdate() {
        assertThrows(AccessDeniedException.class, () -> itemService.updateItem(2L, 10L, updateRequest()));
    }

    @Test
    void adminCanUpdateAnyItem() {
        ItemResponse response = itemService.updateItem(3L, 10L, updateRequest());

        assertEquals("Red Backpack", response.itemName());
    }

    @Test
    void closedItemCannotBeUpdated() {
        item.setStatus(ItemStatus.CLOSED);

        assertThrows(ConflictException.class, () -> itemService.updateItem(1L, 10L, updateRequest()));
    }

    @Test
    void missingItemGivesNotFound() {
        when(itemRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> itemService.getItem(404L));
    }

    @Test
    void ownerCanDeleteItemWithoutClaims() {
        when(claimRepository.existsByItemId(10L)).thenReturn(false);

        itemService.deleteItem(1L, 10L);

        verify(itemRepository).delete(item);
    }

    private static final String OLD_PICTURE = ImageStorageService.URL_PREFIX + "11111111-1111-1111-1111-111111111111.png";
    private static final String NEW_PICTURE = ImageStorageService.URL_PREFIX + "22222222-2222-2222-2222-222222222222.png";

    private ItemRequest updateRequestWithPicture(String imagePath) {
        return new ItemRequest("Red Backpack", "Now red", "Bags", null, "Canteen", null, imagePath);
    }

    @Test
    void removingThePictureDeletesTheOldFile() {
        item.setImagePath(OLD_PICTURE);

        itemService.updateItem(1L, 10L, updateRequestWithPicture(null));

        verify(imageStorageService).deleteByImagePath(OLD_PICTURE);
    }

    @Test
    void replacingThePictureDeletesTheOldFile() {
        item.setImagePath(OLD_PICTURE);

        itemService.updateItem(1L, 10L, updateRequestWithPicture(NEW_PICTURE));

        verify(imageStorageService).deleteByImagePath(OLD_PICTURE);
        verify(imageStorageService, never()).deleteByImagePath(NEW_PICTURE);
    }

    @Test
    void keepingThePictureDeletesNothing() {
        item.setImagePath(OLD_PICTURE);

        itemService.updateItem(1L, 10L, updateRequestWithPicture(OLD_PICTURE));

        verify(imageStorageService, never()).deleteByImagePath(any());
    }

    @Test
    void deletingItemAlsoDeletesItsPicture() {
        String path = ImageStorageService.URL_PREFIX + "11111111-1111-1111-1111-111111111111.png";
        item.setImagePath(path);
        when(claimRepository.existsByItemId(10L)).thenReturn(false);

        itemService.deleteItem(1L, 10L);

        verify(imageStorageService).deleteByImagePath(path);
    }

    @Test
    void picturesAreKeptWhenDeleteIsRefused() {
        item.setImagePath(ImageStorageService.URL_PREFIX + "11111111-1111-1111-1111-111111111111.png");
        when(claimRepository.existsByItemId(10L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> itemService.deleteItem(1L, 10L));
        verify(imageStorageService, never()).deleteByImagePath(any());
    }

    @Test
    void itemWithClaimsCannotBeDeleted() {
        when(claimRepository.existsByItemId(10L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> itemService.deleteItem(1L, 10L));
        verify(itemRepository, never()).delete(any(Item.class));
    }

    @Test
    void otherStudentCannotDelete() {
        assertThrows(AccessDeniedException.class, () -> itemService.deleteItem(2L, 10L));
    }

    @Test
    void ownerCanCloseItemOnlyOnce() {
        ItemResponse closed = itemService.closeItem(1L, 10L);

        assertEquals(ItemStatus.CLOSED, closed.status());
        assertThrows(ConflictException.class, () -> itemService.closeItem(1L, 10L));
    }

    @Test
    void closingAnItemRejectsItsWaitingClaims() {
        Claim waiting = new Claim(item, otherStudent, "It is mine");
        when(claimRepository.findByItemIdAndStatus(10L, ClaimStatus.PENDING)).thenReturn(List.of(waiting));

        itemService.closeItem(1L, 10L);

        assertEquals(ClaimStatus.REJECTED, waiting.getStatus());
        assertEquals(ItemStatus.CLOSED, item.getStatus());
    }

    @Test
    void adminListIncludesInactiveItems() {
        when(itemRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Item>>any(), any(Sort.class)))
                .thenReturn(List.of(item));

        assertEquals(1, itemService.searchAllItemsAsAdmin(null, null, null, null, null).size());
    }
}
