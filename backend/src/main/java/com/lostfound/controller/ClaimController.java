package com.lostfound.controller;

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

import com.lostfound.dto.ClaimRequests.ClaimRequest;
import com.lostfound.dto.ClaimResponse;
import com.lostfound.dto.ItemResponse;
import com.lostfound.service.ClaimService;

import jakarta.validation.Valid;

/** Claim endpoints for students, plus the handover step. */
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

    /** Finder or admin confirms the handover. */
    @PutMapping("/items/{itemId}/returned")
    public ItemResponse markReturned(Authentication authentication, @PathVariable Long itemId) {
        return claimService.markItemReturned(CurrentUser.id(authentication), itemId);
    }
}
