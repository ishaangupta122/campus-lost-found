package com.lostfound.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import com.lostfound.dto.UserResponse;
import com.lostfound.dto.DashboardResponse;
import com.lostfound.model.ClaimStatus;
import com.lostfound.model.ItemStatus;
import com.lostfound.model.ItemType;
import com.lostfound.model.Role;
import com.lostfound.model.User;
import com.lostfound.repository.ClaimRepository;
import com.lostfound.repository.ItemRepository;
import com.lostfound.repository.UserRepository;

class AdminServiceTest {

    private UserRepository userRepository;
    private ItemRepository itemRepository;
    private ClaimRepository claimRepository;
    private AdminService adminService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        itemRepository = mock(ItemRepository.class);
        claimRepository = mock(ClaimRepository.class);
        adminService = new AdminService(userRepository, itemRepository, claimRepository);

        addUser(1L, Role.ADMIN);
        addUser(2L, Role.STUDENT);
    }

    private void addUser(Long id, Role role) {
        User user = mock(User.class);
        when(user.getRole()).thenReturn(role);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
    }

    @Test
    void dashboardContainsAllCounts() {
        when(userRepository.count()).thenReturn(12L);
        when(itemRepository.countByType(ItemType.LOST)).thenReturn(5L);
        when(itemRepository.countByType(ItemType.FOUND)).thenReturn(7L);
        when(itemRepository.countByStatus(ItemStatus.OPEN)).thenReturn(6L);
        when(itemRepository.countByStatus(ItemStatus.CLAIM_PENDING)).thenReturn(2L);
        when(itemRepository.countByStatus(ItemStatus.RETURNED)).thenReturn(3L);
        when(itemRepository.countByStatus(ItemStatus.CLOSED)).thenReturn(1L);
        when(claimRepository.countByStatus(ClaimStatus.PENDING)).thenReturn(4L);

        DashboardResponse dashboard = adminService.getDashboard();

        assertEquals(12, dashboard.totalUsers());
        assertEquals(5, dashboard.totalLostItems());
        assertEquals(7, dashboard.totalFoundItems());
        assertEquals(6, dashboard.openItems());
        assertEquals(2, dashboard.claimPendingItems());
        assertEquals(3, dashboard.returnedItems());
        assertEquals(1, dashboard.closedItems());
        assertEquals(4, dashboard.pendingClaims());
    }

    @Test
    void adminSeesAllUsersWithoutPasswordData() {
        User student = new User("Asha", "asha@thapar.edu", "hash", Role.STUDENT);
        User admin = new User("Admin", "admin@lostfound.com", "hash", Role.ADMIN);
        when(userRepository.findAll(any(Sort.class))).thenReturn(List.of(student, admin));

        List<UserResponse> users = adminService.listUsers();

        assertEquals(2, users.size());
        assertEquals("asha@thapar.edu", users.get(0).email());
        assertEquals(Role.STUDENT, users.get(0).role());
        assertFalse(users.get(0).googleAccount());
    }
}
