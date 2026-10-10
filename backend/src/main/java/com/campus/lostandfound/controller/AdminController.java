package com.campus.lostandfound.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.campus.lostandfound.dto.AdminUserResponse;
import com.campus.lostandfound.dto.ClaimDecisionRequest;
import com.campus.lostandfound.dto.ClaimResponse;
import com.campus.lostandfound.dto.DashboardResponse;
import com.campus.lostandfound.dto.ItemResponse;
import com.campus.lostandfound.model.ItemStatus;
import com.campus.lostandfound.model.ItemType;
import com.campus.lostandfound.service.AdminService;
import com.campus.lostandfound.service.ClaimService;
import com.campus.lostandfound.service.ItemService;

import jakarta.validation.Valid;

/**
 * Everything under /api/admin needs the ADMIN role: SecurityConfig rejects other
 * users with 403 before this class runs, and the services check the role again.
 */
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
    public DashboardResponse dashboard(Authentication authentication) {
        return adminService.getDashboard(CurrentUser.id(authentication));
    }

    /** All registered users, newest first (no password data). */
    @GetMapping("/users")
    public List<AdminUserResponse> users(Authentication authentication) {
        return adminService.listUsers(CurrentUser.id(authentication));
    }

    @GetMapping("/claims/pending")
    public List<ClaimResponse> pendingClaims(Authentication authentication) {
        return claimService.listPendingClaims(CurrentUser.id(authentication));
    }

    @PutMapping("/claims/{id}/decision")
    public ClaimResponse decide(Authentication authentication, @PathVariable Long id,
                                @Valid @RequestBody ClaimDecisionRequest request) {
        return claimService.decideClaim(CurrentUser.id(authentication), id, request.decision());
    }

    /** All reports in any status (the public list shows only active ones). */
    @GetMapping("/items")
    public List<ItemResponse> allItems(Authentication authentication,
                                       @RequestParam(required = false) String q,
                                       @RequestParam(required = false) ItemType type,
                                       @RequestParam(required = false) String category,
                                       @RequestParam(required = false) String location,
                                       @RequestParam(required = false) ItemStatus status) {
        return itemService.searchAllItemsAsAdmin(CurrentUser.id(authentication), q, type, category, location, status);
    }

    /** Moderation: close an inappropriate or resolved report. */
    @PutMapping("/items/{id}/close")
    public ItemResponse closeItem(Authentication authentication, @PathVariable Long id) {
        return itemService.closeItem(CurrentUser.id(authentication), id);
    }

    /** Admin confirms the handover of an item whose claim was approved. */
    @PutMapping("/items/{id}/returned")
    public ItemResponse markReturned(Authentication authentication, @PathVariable Long id) {
        return claimService.markItemReturned(CurrentUser.id(authentication), id);
    }
}
