package com.campus.lostandfound.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.campus.lostandfound.config.SecurityConfig;
import com.campus.lostandfound.dto.AdminUserResponse;
import com.campus.lostandfound.dto.ClaimResponse;
import com.campus.lostandfound.dto.DashboardResponse;
import com.campus.lostandfound.exception.ConflictException;
import com.campus.lostandfound.model.ClaimStatus;
import com.campus.lostandfound.model.ItemStatus;
import com.campus.lostandfound.model.Role;
import com.campus.lostandfound.service.AdminService;
import com.campus.lostandfound.service.ClaimService;
import com.campus.lostandfound.service.ItemService;

/** Proves that /api/admin/** is closed to students and anonymous users. */
@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminService adminService;

    @MockitoBean
    private ClaimService claimService;

    @MockitoBean
    private ItemService itemService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private RequestPostProcessor user(long id, String role) {
        return jwt().jwt(token -> token.subject(String.valueOf(id)).claim("role", role))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private ClaimResponse approvedClaim() {
        return new ClaimResponse(5L, 10L, "Keys", ItemStatus.CLAIM_PENDING, 2L, "Claimant",
                "mine", ClaimStatus.APPROVED, LocalDateTime.now());
    }

    @Test
    void adminSeesDashboard() throws Exception {
        when(adminService.getDashboard(1L)).thenReturn(new DashboardResponse(12, 5, 7, 6, 2, 3, 1, 4));

        mockMvc.perform(get("/api/admin/dashboard").with(user(1, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(12))
                .andExpect(jsonPath("$.pendingClaims").value(4));
    }

    @Test
    void adminSeesUserList() throws Exception {
        when(adminService.listUsers(1L)).thenReturn(List.of(
                new AdminUserResponse(2L, "Asha", "asha@campus.edu", Role.STUDENT, false, LocalDateTime.now())));

        mockMvc.perform(get("/api/admin/users").with(user(1, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("asha@campus.edu"))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    }

    @Test
    void studentGets403OnUserList() throws Exception {
        mockMvc.perform(get("/api/admin/users").with(user(2, "STUDENT"))).andExpect(status().isForbidden());
    }

    @Test
    void studentGets403OnEveryAdminUrl() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard").with(user(2, "STUDENT"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/claims/pending").with(user(2, "STUDENT"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/items").with(user(2, "STUDENT"))).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/admin/claims/5/decision").with(user(2, "STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVED\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/admin/items/10/close").with(user(2, "STUDENT"))).andExpect(status().isForbidden());
    }

    @Test
    void anonymousGets401() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminSeesPendingClaims() throws Exception {
        when(claimService.listPendingClaims(1L)).thenReturn(List.of(approvedClaim()));

        mockMvc.perform(get("/api/admin/claims/pending").with(user(1, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5));
    }

    @Test
    void adminCanApproveClaim() throws Exception {
        when(claimService.decideClaim(1L, 5L, ClaimStatus.APPROVED)).thenReturn(approvedClaim());

        mockMvc.perform(put("/api/admin/claims/5/decision").with(user(1, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void missingOrUnknownDecisionGives400() throws Exception {
        mockMvc.perform(put("/api/admin/claims/5/decision").with(user(1, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.decision").exists());
        mockMvc.perform(put("/api/admin/claims/5/decision").with(user(1, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"MAYBE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void decidingTwiceGives409() throws Exception {
        when(claimService.decideClaim(eq(1L), eq(5L), eq(ClaimStatus.REJECTED)))
                .thenThrow(new ConflictException("This claim was already APPROVED"));

        mockMvc.perform(put("/api/admin/claims/5/decision").with(user(1, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"REJECTED\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void adminItemListAcceptsFilters() throws Exception {
        when(itemService.searchAllItemsAsAdmin(1L, "bag", null, null, null, ItemStatus.CLOSED))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/admin/items").param("q", "bag").param("status", "CLOSED")
                        .with(user(1, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
