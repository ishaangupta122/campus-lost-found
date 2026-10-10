package com.campus.lostandfound.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campus.lostandfound.dto.AdminUserResponse;
import com.campus.lostandfound.dto.DashboardResponse;
import com.campus.lostandfound.exception.ForbiddenException;
import com.campus.lostandfound.exception.UnauthorizedException;
import com.campus.lostandfound.model.ClaimStatus;
import com.campus.lostandfound.model.ItemStatus;
import com.campus.lostandfound.model.ItemType;
import com.campus.lostandfound.model.Role;
import com.campus.lostandfound.model.User;
import com.campus.lostandfound.repository.ClaimRepository;
import com.campus.lostandfound.repository.ItemRepository;
import com.campus.lostandfound.repository.UserRepository;

/**
 * Dashboard statistics. Claim decisions and item moderation reuse ClaimService
 * and ItemService, so the rules exist in exactly one place.
 *
 * Admin-only is enforced twice (defence in depth): the URL rule in SecurityConfig
 * AND the role check here.
 */
@Service
@Transactional(readOnly = true)
public class AdminService {

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final ClaimRepository claimRepository;

    public AdminService(UserRepository userRepository, ItemRepository itemRepository,
                        ClaimRepository claimRepository) {
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
        this.claimRepository = claimRepository;
    }

    public DashboardResponse getDashboard(Long adminId) {
        requireAdmin(adminId, "Only admins can view the dashboard");
        return new DashboardResponse(
                userRepository.count(),
                itemRepository.countByType(ItemType.LOST),
                itemRepository.countByType(ItemType.FOUND),
                itemRepository.countByStatus(ItemStatus.OPEN),
                itemRepository.countByStatus(ItemStatus.CLAIM_PENDING),
                itemRepository.countByStatus(ItemStatus.RETURNED),
                itemRepository.countByStatus(ItemStatus.CLOSED),
                claimRepository.countByStatus(ClaimStatus.PENDING));
    }

    /** Every registered user (students and admins), newest first. */
    public List<AdminUserResponse> listUsers(Long adminId) {
        requireAdmin(adminId, "Only admins can view the user list");
        return userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(AdminUserResponse::from)
                .toList();
    }

    private void requireAdmin(Long adminId, String message) {
        User actor = userRepository.findById(adminId)
                .orElseThrow(() -> new UnauthorizedException("Unknown user"));
        if (actor.getRole() != Role.ADMIN) {
            throw new ForbiddenException(message);
        }
    }
}
