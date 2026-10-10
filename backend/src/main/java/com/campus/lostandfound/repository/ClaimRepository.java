package com.campus.lostandfound.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.campus.lostandfound.model.Claim;
import com.campus.lostandfound.model.ClaimStatus;

public interface ClaimRepository extends JpaRepository<Claim, Long> {

    /** A student's own claims, newest first (GET /api/claims/my). */
    List<Claim> findByClaimantIdOrderByCreatedAtDesc(Long claimantId);

    /** Claims waiting for an admin, oldest first. */
    List<Claim> findByStatusOrderByCreatedAtAsc(ClaimStatus status);

    /** Used to block a student from claiming the same item twice. */
    boolean existsByItemIdAndClaimantIdAndStatus(Long itemId, Long claimantId, ClaimStatus status);

    /** Used to block a second approval for the same item. */
    boolean existsByItemIdAndStatus(Long itemId, ClaimStatus status);

    /** All claims of one item in the given state (e.g. its pending claims). */
    List<Claim> findByItemIdAndStatus(Long itemId, ClaimStatus status);

    /** True if anybody ever claimed this item. */
    boolean existsByItemId(Long itemId);

    long countByStatus(ClaimStatus status);
}
