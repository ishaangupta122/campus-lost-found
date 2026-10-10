package com.campus.lostandfound.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.campus.lostandfound.dto.DashboardResponse;
import com.campus.lostandfound.exception.ForbiddenException;
import com.campus.lostandfound.model.ClaimStatus;
import com.campus.lostandfound.model.ItemStatus;
import com.campus.lostandfound.model.ItemType;
import com.campus.lostandfound.model.Role;
import com.campus.lostandfound.model.User;
import com.campus.lostandfound.repository.ClaimRepository;
import com.campus.lostandfound.repository.ItemRepository;
import com.campus.lostandfound.repository.UserRepository;

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

        DashboardResponse dashboard = adminService.getDashboard(1L);

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
    void studentCannotSeeDashboard() {
        assertThrows(ForbiddenException.class, () -> adminService.getDashboard(2L));
    }
}
