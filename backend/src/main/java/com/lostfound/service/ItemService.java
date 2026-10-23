package com.lostfound.service;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.lostfound.repository.ItemSpecifications;
import com.lostfound.repository.UserRepository;

/** Business rules for lost/found reports. Returns DTOs, not entities. */
@Service
@Transactional
public class ItemService {

    /** Statuses shown in the public list. */
    private static final List<ItemStatus> ACTIVE_STATUSES = List.of(ItemStatus.OPEN, ItemStatus.CLAIM_PENDING);

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final ClaimRepository claimRepository;
    private final ImageStorageService imageStorageService;

    public ItemService(ItemRepository itemRepository, UserRepository userRepository,
                       ClaimRepository claimRepository, ImageStorageService imageStorageService) {
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.claimRepository = claimRepository;
        this.imageStorageService = imageStorageService;
    }

    public ItemResponse createItem(Long userId, ItemRequest request) {
        User actor = loadUser(userId);
        if (actor.getRole() != Role.STUDENT) {
            throw new AccessDeniedException("Only students can create reports");
        }
        if (request.type() == null) {
            throw new BadRequestException("Type is required (LOST or FOUND)");
        }
        LocalDate date = request.reportDate() != null ? request.reportDate() : LocalDate.now();
        Item item = new Item(request.itemName().trim(), request.description().trim(),
                request.category().trim(), request.type(), request.location().trim(), date, actor);
        item.setImagePath(request.imagePath());
        return ItemResponse.from(itemRepository.save(item));
    }

    @Transactional(readOnly = true)
    public ItemResponse getItem(Long itemId) {
        return ItemResponse.from(findItem(itemId));
    }

    /** Public list: active items unless a status is given; all filters optional. */
    @Transactional(readOnly = true)
    public List<ItemResponse> searchItems(String keyword, ItemType type, String category,
                                          String location, ItemStatus status) {
        List<ItemStatus> statuses = (status != null) ? List.of(status) : ACTIVE_STATUSES;
        return search(keyword, type, category, location, statuses);
    }

    /** Admin list: every status. */
    @Transactional(readOnly = true)
    public List<ItemResponse> searchAllItemsAsAdmin(String keyword, ItemType type,
                                                    String category, String location, ItemStatus status) {
        List<ItemStatus> statuses = (status != null) ? List.of(status) : List.of(ItemStatus.values());
        return search(keyword, type, category, location, statuses);
    }

    private List<ItemResponse> search(String keyword, ItemType type, String category,
                                      String location, List<ItemStatus> statuses) {
        Specification<Item> spec = ItemSpecifications.search(keyword, type, category, location, statuses);
        return itemRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(ItemResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ItemResponse> getMyItems(Long userId) {
        User actor = loadUser(userId);
        return itemRepository.findByReporterIdOrderByCreatedAtDesc(actor.getId())
                .stream()
                .map(ItemResponse::from)
                .toList();
    }

    public ItemResponse updateItem(Long userId, Long itemId, ItemRequest request) {
        User actor = loadUser(userId);
        Item item = findItem(itemId);
        requireOwnerOrAdmin(actor, item);
        if (!item.isActive()) {
            throw new ConflictException("A returned or closed report can no longer be edited");
        }
        item.setItemName(request.itemName().trim());
        item.setDescription(request.description().trim());
        item.setCategory(request.category().trim());
        item.setLocation(request.location().trim());
        if (request.reportDate() != null) {
            item.setReportDate(request.reportDate());
        }
        String oldImagePath = item.getImagePath();
        item.setImagePath(request.imagePath());
        ItemResponse response = ItemResponse.from(itemRepository.save(item));
        // old picture no longer needed
        if (oldImagePath != null && !oldImagePath.equals(request.imagePath())) {
            imageStorageService.deleteByImagePath(oldImagePath);
        }
        return response;
    }

    public void deleteItem(Long userId, Long itemId) {
        User actor = loadUser(userId);
        Item item = findItem(itemId);
        requireOwnerOrAdmin(actor, item);
        if (claimRepository.existsByItemId(itemId)) {
            throw new ConflictException("This report has claims, so it cannot be deleted. Close it instead.");
        }
        String imagePath = item.getImagePath();
        itemRepository.delete(item);
        imageStorageService.deleteByImagePath(imagePath);
    }

    /** Owner or admin closes a report. */
    public ItemResponse closeItem(Long userId, Long itemId) {
        User actor = loadUser(userId);
        // lock the row: no claim decision at the same time
        Item item = itemRepository.findByIdForUpdate(itemId)
                .orElseThrow(() -> new NoSuchElementException("Item " + itemId + " not found"));
        requireOwnerOrAdmin(actor, item);
        if (!item.isActive()) {
            throw new ConflictException("This report is already " + item.getStatus());
        }
        // waiting claims are rejected
        List<Claim> waiting = claimRepository.findByItemIdAndStatus(itemId, ClaimStatus.PENDING);
        waiting.forEach(claim -> claim.setStatus(ClaimStatus.REJECTED));
        claimRepository.saveAll(waiting);

        item.setStatus(ItemStatus.CLOSED);
        return ItemResponse.from(itemRepository.save(item));
    }


    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Unknown user"));
    }

    private Item findItem(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new NoSuchElementException("Item " + itemId + " not found"));
    }

    private void requireOwnerOrAdmin(User actor, Item item) {
        boolean isOwner = item.getReporter().getId().equals(actor.getId());
        boolean isAdmin = actor.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You can only change your own reports");
        }
    }
}
