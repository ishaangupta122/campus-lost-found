package com.lostfound.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lostfound.model.Claim;
import com.lostfound.model.ClaimStatus;

public interface ClaimRepository extends JpaRepository<Claim, Long> {

    /** A student's claims, newest first. */
    List<Claim> findByClaimantIdOrderByCreatedAtDesc(Long claimantId);

    /** Pending claims, oldest first. */
    List<Claim> findByStatusOrderByCreatedAtAsc(ClaimStatus status);

    /** Blocks a duplicate claim by the same student. */
    boolean existsByItemIdAndClaimantIdAndStatus(Long itemId, Long claimantId, ClaimStatus status);

    /** Blocks a second approval. */
    boolean existsByItemIdAndStatus(Long itemId, ClaimStatus status);

    /** Claims of one item in a given state. */
    List<Claim> findByItemIdAndStatus(Long itemId, ClaimStatus status);

    /** Has anybody claimed this item? */
    boolean existsByItemId(Long itemId);

    long countByStatus(ClaimStatus status);
}
