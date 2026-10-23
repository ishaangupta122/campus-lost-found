package com.lostfound.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lostfound.dto.ClaimRequests.ClaimRequest;
import com.lostfound.dto.ClaimResponse;
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

/**
 * Claim workflow: OPEN -> CLAIM_PENDING -> (admin approves) -> RETURNED once the handover is confirmed.
 * If every waiting claim is rejected the item goes back to OPEN.
 * @Transactional: a claim and its item change together. Item rows are locked first (findByIdForUpdate).
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

    // ---- students ----

    public ClaimResponse submitClaim(Long userId, Long itemId, ClaimRequest request) {
        User actor = loadUser(userId);
        if (actor.getRole() != Role.STUDENT) {
            throw new AccessDeniedException("Only students can submit claims");
        }
        Item item = lockItem(itemId);

        if (item.getType() != ItemType.FOUND) {
            throw new BadRequestException("Only found items can be claimed");
        }
        if (!item.isActive()) {
            throw new ConflictException("This item is " + item.getStatus() + " and can no longer be claimed");
        }
        if (item.getReporter().getId().equals(actor.getId())) {
            throw new AccessDeniedException("You cannot claim your own report");
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

    /** Claimant, finder or admin only. */
    @Transactional(readOnly = true)
    public ClaimResponse getClaim(Long userId, Long claimId) {
        User actor = loadUser(userId);
        Claim claim = findClaim(claimId);
        boolean isClaimant = claim.getClaimant().getId().equals(actor.getId());
        boolean isFinder = claim.getItem().getReporter().getId().equals(actor.getId());
        if (!isClaimant && !isFinder && actor.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("You are not allowed to view this claim");
        }
        return ClaimResponse.from(claim);
    }

    // ---- admins ----

    @Transactional(readOnly = true)
    public List<ClaimResponse> listPendingClaims() {
        return claimRepository.findByStatusOrderByCreatedAtAsc(ClaimStatus.PENDING)
                .stream()
                .map(ClaimResponse::from)
                .toList();
    }

    /** Admin approves or rejects a pending claim. */
    public ClaimResponse decideClaim(Long claimId, ClaimStatus decision) {
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

    /** Finder or admin confirms the handover. */
    public ItemResponse markItemReturned(Long userId, Long itemId) {
        User actor = loadUser(userId);
        Item item = lockItem(itemId);
        boolean isFinder = item.getReporter().getId().equals(actor.getId());
        if (!isFinder && actor.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only the person who reported the item, or an admin, can mark it returned");
        }
        boolean hasApprovedClaim = claimRepository.existsByItemIdAndStatus(itemId, ClaimStatus.APPROVED);
        if (item.getStatus() != ItemStatus.CLAIM_PENDING || !hasApprovedClaim) {
            throw new ConflictException("An item can be marked returned only after one of its claims is approved");
        }
        item.setStatus(ItemStatus.RETURNED);
        return ItemResponse.from(itemRepository.save(item));
    }

    // ---- helpers ----

    private void approve(Claim claim, Item item) {
        if (claimRepository.existsByItemIdAndStatus(item.getId(), ClaimStatus.APPROVED)) {
            throw new ConflictException("This item already has an approved claim");
        }
        claim.setStatus(ClaimStatus.APPROVED);
        claimRepository.save(claim);

        // only one claim wins: reject the others
        List<Claim> others = claimRepository.findByItemIdAndStatus(item.getId(), ClaimStatus.PENDING);
        for (Claim other : others) {
            if (!other.getId().equals(claim.getId())) {
                other.setStatus(ClaimStatus.REJECTED);
            }
        }
        claimRepository.saveAll(others);

        item.setStatus(ItemStatus.CLAIM_PENDING); // waits for the handover
        itemRepository.save(item);
    }

    private void reject(Claim claim, Item item) {
        claim.setStatus(ClaimStatus.REJECTED);
        claimRepository.save(claim);

        boolean otherWaiting = claimRepository.existsByItemIdAndStatus(item.getId(), ClaimStatus.PENDING);
        boolean approved = claimRepository.existsByItemIdAndStatus(item.getId(), ClaimStatus.APPROVED);
        if (!otherWaiting && !approved) {
            item.setStatus(ItemStatus.OPEN);
            itemRepository.save(item);
        }
    }

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Unknown user"));
    }

    private Claim findClaim(Long claimId) {
        return claimRepository.findById(claimId)
                .orElseThrow(() -> new NoSuchElementException("Claim " + claimId + " not found"));
    }

    private Item lockItem(Long itemId) {
        return itemRepository.findByIdForUpdate(itemId)
                .orElseThrow(() -> new NoSuchElementException("Item " + itemId + " not found"));
    }
}
