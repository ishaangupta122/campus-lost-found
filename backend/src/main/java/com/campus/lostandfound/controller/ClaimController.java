package com.campus.lostandfound.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campus.lostandfound.dto.ClaimRequest;
import com.campus.lostandfound.dto.ClaimResponse;
import com.campus.lostandfound.dto.ItemResponse;
import com.campus.lostandfound.service.ClaimService;

import jakarta.validation.Valid;

/**
 * Student-facing claim endpoints. The admin endpoints (pending list and
 * approve/reject) are added in Stage 6 under /api/admin/claims.
 */
@RestController
@RequestMapping("/api")
public class ClaimController {

    private final ClaimService claimService;

    public ClaimController(ClaimService claimService) {
        this.claimService = claimService;
    }

    @PostMapping("/items/{itemId}/claims")
    public ResponseEntity<ClaimResponse> submit(Authentication authentication,
                                                @PathVariable Long itemId,
                                                @Valid @RequestBody ClaimRequest request) {
        ClaimResponse created = claimService.submitClaim(CurrentUser.id(authentication), itemId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/claims/my")
    public List<ClaimResponse> myClaims(Authentication authentication) {
        return claimService.getMyClaims(CurrentUser.id(authentication));
    }

    @GetMapping("/claims/{id}")
    public ClaimResponse getById(Authentication authentication, @PathVariable Long id) {
        return claimService.getClaim(CurrentUser.id(authentication), id);
    }

    /** Handover finished: finder or admin marks the item RETURNED. */
    @PutMapping("/items/{itemId}/returned")
    public ItemResponse markReturned(Authentication authentication, @PathVariable Long itemId) {
        return claimService.markItemReturned(CurrentUser.id(authentication), itemId);
    }
}
