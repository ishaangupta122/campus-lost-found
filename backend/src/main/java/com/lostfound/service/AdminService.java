package com.lostfound.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lostfound.dto.UserResponse;
import com.lostfound.dto.DashboardResponse;
import com.lostfound.model.ClaimStatus;
import com.lostfound.model.ItemStatus;
import com.lostfound.model.ItemType;
import com.lostfound.repository.ClaimRepository;
import com.lostfound.repository.ItemRepository;
import com.lostfound.repository.UserRepository;

/** Dashboard counts and the user list. Admin-only is enforced by the URL rule in SecurityConfig. */
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

    public DashboardResponse getDashboard() {
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

    public List<UserResponse> listUsers() {
        return userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(UserResponse::from)
                .toList();
    }
}
