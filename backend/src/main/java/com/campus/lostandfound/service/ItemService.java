package com.campus.lostandfound.service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.campus.lostandfound.dto.ItemCreateRequest;
import com.campus.lostandfound.dto.ItemResponse;
import com.campus.lostandfound.dto.ItemUpdateRequest;
import com.campus.lostandfound.exception.ConflictException;
import com.campus.lostandfound.exception.ForbiddenException;
import com.campus.lostandfound.exception.ResourceNotFoundException;
import com.campus.lostandfound.exception.UnauthorizedException;
import com.campus.lostandfound.model.Claim;
import com.campus.lostandfound.model.ClaimStatus;
import com.campus.lostandfound.model.Item;
import com.campus.lostandfound.model.ItemStatus;
import com.campus.lostandfound.model.ItemType;
import com.campus.lostandfound.model.Role;
import com.campus.lostandfound.model.User;
import com.campus.lostandfound.repository.ClaimRepository;
import com.campus.lostandfound.repository.ItemRepository;
import com.campus.lostandfound.repository.ItemSpecifications;
import com.campus.lostandfound.repository.UserRepository;

/**
 * All business rules for lost/found reports live here (never in the controller).
 *
 * Methods return DTOs, not entities. They are @Transactional so that lazily
 * loaded data (the reporter) can still be read while we build the response.
 */
@Service
@Transactional
public class ItemService {

    /** OPEN and CLAIM_PENDING: taken from the enum so the rule exists in only one place. */
    private static final List<ItemStatus> ACTIVE_STATUSES = Arrays.stream(ItemStatus.values())
            .filter(ItemStatus::isActive)
            .toList();

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

    public ItemResponse createItem(Long userId, ItemCreateRequest request) {
        User actor = loadUser(userId);
        if (actor.getRole() != Role.STUDENT) {
            throw new ForbiddenException("Only students can create reports");
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

    /** Active listings by default; every filter is optional. */
    @Transactional(readOnly = true)
    public List<ItemResponse> searchItems(String keyword, ItemType type, String category,
                                          String location, ItemStatus status) {
        List<ItemStatus> statuses = (status != null) ? List.of(status) : ACTIVE_STATUSES;
        return search(keyword, type, category, location, statuses);
    }

    /** Admin view: every report, whatever its status, with the same optional filters. */
    @Transactional(readOnly = true)
    public List<ItemResponse> searchAllItemsAsAdmin(Long adminId, String keyword, ItemType type,
                                                    String category, String location, ItemStatus status) {
        User actor = loadUser(adminId);
        if (actor.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only admins can view all reports");
        }
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

    public ItemResponse updateItem(Long userId, Long itemId, ItemUpdateRequest request) {
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
        // The picture was removed or replaced: its old file is no longer needed.
        if (oldImagePath != null && !oldImagePath.equals(request.imagePath())) {
            deleteImageAfterCommit(oldImagePath);
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
        deleteImageAfterCommit(imagePath);
    }

    /**
     * Removes the picture file only after the database delete is committed, so a rolled-back
     * delete never leaves a report whose picture is gone. Without a transaction (unit tests)
     * it deletes straight away.
     */
    private void deleteImageAfterCommit(String imagePath) {
        if (imagePath == null) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    imageStorageService.deleteByImagePath(imagePath);
                }
            });
        } else {
            imageStorageService.deleteByImagePath(imagePath);
        }
    }

    /** Owner or admin closes a report (e.g. the owner found the item, or an admin removes a bad post). */
    public ItemResponse closeItem(Long userId, Long itemId) {
        User actor = loadUser(userId);
        // Lock the row so a claim decision cannot happen at the same moment.
        Item item = itemRepository.findByIdForUpdate(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item " + itemId + " not found"));
        requireOwnerOrAdmin(actor, item);
        if (!item.isActive()) {
            throw new ConflictException("This report is already " + item.getStatus());
        }
        // Nobody can decide on claims of a closed item, so waiting claims are rejected.
        List<Claim> waiting = claimRepository.findByItemIdAndStatus(itemId, ClaimStatus.PENDING);
        waiting.forEach(claim -> claim.setStatus(ClaimStatus.REJECTED));
        claimRepository.saveAll(waiting);

        item.setStatus(ItemStatus.CLOSED);
        return ItemResponse.from(itemRepository.save(item));
    }

    // ----- small helper methods keep the public methods short -----

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Unknown user"));
    }

    private Item findItem(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item " + itemId + " not found"));
    }

    private void requireOwnerOrAdmin(User actor, Item item) {
        boolean isOwner = item.getReporter().getId().equals(actor.getId());
        boolean isAdmin = actor.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("You can only change your own reports");
        }
    }
}
