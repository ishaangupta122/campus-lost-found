package com.lostfound.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lostfound.dto.UserResponse;
import com.lostfound.dto.ClaimRequests.ClaimDecisionRequest;
import com.lostfound.dto.ClaimResponse;
import com.lostfound.dto.DashboardResponse;
import com.lostfound.dto.ItemResponse;
import com.lostfound.model.ItemStatus;
import com.lostfound.model.ItemType;
import com.lostfound.service.AdminService;
import com.lostfound.service.ClaimService;
import com.lostfound.service.ItemService;

import jakarta.validation.Valid;

/** Admin-only endpoints (the URL rule is in SecurityConfig). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final ClaimService claimService;
    private final ItemService itemService;

    public AdminController(AdminService adminService, ClaimService claimService, ItemService itemService) {
        this.adminService = adminService;
        this.claimService = claimService;
        this.itemService = itemService;
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard() {
        return adminService.getDashboard();
    }

    @GetMapping("/users")
    public List<UserResponse> users() {
        return adminService.listUsers();
    }

    @GetMapping("/claims/pending")
    public List<ClaimResponse> pendingClaims() {
        return claimService.listPendingClaims();
    }

    @PutMapping("/claims/{id}/decision")
    public ClaimResponse decide(@PathVariable Long id,
                                @Valid @RequestBody ClaimDecisionRequest request) {
        return claimService.decideClaim(id, request.decision());
    }

    /** All reports, any status. */
    @GetMapping("/items")
    public List<ItemResponse> allItems(@RequestParam(required = false) String q,
                                       @RequestParam(required = false) ItemType type,
                                       @RequestParam(required = false) String category,
                                       @RequestParam(required = false) String location,
                                       @RequestParam(required = false) ItemStatus status) {
        return itemService.searchAllItemsAsAdmin(q, type, category, location, status);
    }

    @PutMapping("/items/{id}/close")
    public ItemResponse closeItem(Authentication authentication, @PathVariable Long id) {
        return itemService.closeItem(CurrentUser.id(authentication), id);
    }

    @PutMapping("/items/{id}/returned")
    public ItemResponse markReturned(Authentication authentication, @PathVariable Long id) {
        return claimService.markItemReturned(CurrentUser.id(authentication), id);
    }
}
