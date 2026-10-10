package com.campus.lostandfound.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campus.lostandfound.dto.ClaimRequest;
import com.campus.lostandfound.dto.ClaimResponse;
import com.campus.lostandfound.dto.ItemResponse;
import com.campus.lostandfound.exception.BadRequestException;
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
import com.campus.lostandfound.repository.UserRepository;

/**
 * The claim workflow.
 *
 * Life of a found item:
 *   OPEN --(first claim submitted)--> CLAIM_PENDING --(admin approves a claim)--> CLAIM_PENDING (waiting for handover)
 *        --(finder or admin confirms handover)--> RETURNED
 *   If every waiting claim is rejected the item goes back to OPEN.
 *
 * Every method is @Transactional, so a claim and its item always change together:
 * either both changes are saved or neither is. Methods that change an item first
 * LOCK its row (findByIdForUpdate), so simultaneous requests are handled one by one.
 */
@Service
@Transactional
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    public ClaimService(ClaimRepository claimRepository, ItemRepository itemRepository,
                        UserRepository userRepository) {
        this.claimRepository = claimRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
    }

    // ------------------------------------------------------------------ students

    public ClaimResponse submitClaim(Long userId, Long itemId, ClaimRequest request) {
        User actor = loadUser(userId);
        if (actor.getRole() != Role.STUDENT) {
            throw new ForbiddenException("Only students can submit claims");
        }
        Item item = lockItem(itemId);

        if (item.getType() != ItemType.FOUND) {
            throw new BadRequestException("Only found items can be claimed");
        }
        if (!item.isActive()) {
            throw new ConflictException("This item is " + item.getStatus() + " and can no longer be claimed");
        }
        if (item.getReporter().getId().equals(actor.getId())) {
            throw new ForbiddenException("You cannot claim your own report");
        }
        if (claimRepository.existsByItemIdAndStatus(itemId, ClaimStatus.APPROVED)) {
            throw new ConflictException("This item already has an approved claim");
        }
        if (claimRepository.existsByItemIdAndClaimantIdAndStatus(itemId, actor.getId(), ClaimStatus.PENDING)) {
            throw new ConflictException("You already have a pending claim for this item");
        }

        Claim claim = claimRepository.save(new Claim(item, actor, request.message().trim()));
        item.setStatus(ItemStatus.CLAIM_PENDING);
        itemRepository.save(item);
        return ClaimResponse.from(claim);
    }

    @Transactional(readOnly = true)
    public List<ClaimResponse> getMyClaims(Long userId) {
        User actor = loadUser(userId);
        return claimRepository.findByClaimantIdOrderByCreatedAtDesc(actor.getId())
                .stream()
                .map(ClaimResponse::from)
                .toList();
    }

    /** Visible to the claimant, to the person who reported the item, and to admins. */
    @Transactional(readOnly = true)
    public ClaimResponse getClaim(Long userId, Long claimId) {
        User actor = loadUser(userId);
        Claim claim = findClaim(claimId);
        boolean isClaimant = claim.getClaimant().getId().equals(actor.getId());
        boolean isFinder = claim.getItem().getReporter().getId().equals(actor.getId());
        if (!isClaimant && !isFinder && actor.getRole() != Role.ADMIN) {
            throw new ForbiddenException("You are not allowed to view this claim");
        }
        return ClaimResponse.from(claim);
    }

    // -------------------------------------------------------------------- admins

    @Transactional(readOnly = true)
    public List<ClaimResponse> listPendingClaims(Long adminId) {
        requireAdmin(loadUser(adminId));
        return claimRepository.findByStatusOrderByCreatedAtAsc(ClaimStatus.PENDING)
                .stream()
                .map(ClaimResponse::from)
                .toList();
    }

    /** Admin approves or rejects a pending claim. */
    public ClaimResponse decideClaim(Long adminId, Long claimId, ClaimStatus decision) {
        requireAdmin(loadUser(adminId));
        if (decision != ClaimStatus.APPROVED && decision != ClaimStatus.REJECTED) {
            throw new BadRequestException("Decision must be APPROVED or REJECTED");
        }
        Claim claim = findClaim(claimId);
        Item item = lockItem(claim.getItem().getId());

        if (claim.getStatus() != ClaimStatus.PENDING) {
            throw new ConflictException("This claim was already " + claim.getStatus());
        }
        if (!item.isActive()) {
            throw new ConflictException("The item is " + item.getStatus() + ", so its claims can no longer be decided");
        }

        if (decision == ClaimStatus.APPROVED) {
            approve(claim, item);
        } else {
            reject(claim, item);
        }
        return ClaimResponse.from(claim);
    }

    /** Handover done: the finder (item reporter) or an admin marks the item RETURNED. */
    public ItemResponse markItemReturned(Long userId, Long itemId) {
        User actor = loadUser(userId);
        Item item = lockItem(itemId);
        boolean isFinder = item.getReporter().getId().equals(actor.getId());
        if (!isFinder && actor.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only the person who reported the item, or an admin, can mark it returned");
        }
        boolean hasApprovedClaim = claimRepository.existsByItemIdAndStatus(itemId, ClaimStatus.APPROVED);
        if (item.getStatus() != ItemStatus.CLAIM_PENDING || !hasApprovedClaim) {
            throw new ConflictException("An item can be marked returned only after one of its claims is approved");
        }
        item.setStatus(ItemStatus.RETURNED);
        return ItemResponse.from(itemRepository.save(item));
    }

    // ------------------------------------------------------------------- helpers

    private void approve(Claim claim, Item item) {
        if (claimRepository.existsByItemIdAndStatus(item.getId(), ClaimStatus.APPROVED)) {
            throw new ConflictException("This item already has an approved claim");
        }
        claim.setStatus(ClaimStatus.APPROVED);
        claimRepository.save(claim);

        // Only one claim can win: every other waiting claim for this item is rejected.
        List<Claim> others = claimRepository.findByItemIdAndStatus(item.getId(), ClaimStatus.PENDING);
        for (Claim other : others) {
            if (!other.getId().equals(claim.getId())) {
                other.setStatus(ClaimStatus.REJECTED);
            }
        }
        claimRepository.saveAll(others);

        item.setStatus(ItemStatus.CLAIM_PENDING); // stays pending until the handover is confirmed
        itemRepository.save(item);
    }

    private void reject(Claim claim, Item item) {
        claim.setStatus(ClaimStatus.REJECTED);
        claimRepository.save(claim);

        boolean otherWaiting = claimRepository.existsByItemIdAndStatus(item.getId(), ClaimStatus.PENDING);
        boolean approved = claimRepository.existsByItemIdAndStatus(item.getId(), ClaimStatus.APPROVED);
        if (!otherWaiting && !approved) {
            item.setStatus(ItemStatus.OPEN); // nobody is waiting any more
            itemRepository.save(item);
        }
    }

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Unknown user"));
    }

    private Claim findClaim(Long claimId) {
        return claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim " + claimId + " not found"));
    }

    private Item lockItem(Long itemId) {
        return itemRepository.findByIdForUpdate(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item " + itemId + " not found"));
    }

    private void requireAdmin(User actor) {
        if (actor.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only admins can do this");
        }
    }
}
