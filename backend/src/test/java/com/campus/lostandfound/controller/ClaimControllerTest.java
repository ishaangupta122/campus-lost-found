package com.campus.lostandfound.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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
import com.campus.lostandfound.dto.ClaimRequest;
import com.campus.lostandfound.dto.ClaimResponse;
import com.campus.lostandfound.dto.ItemResponse;
import com.campus.lostandfound.exception.ConflictException;
import com.campus.lostandfound.exception.ForbiddenException;
import com.campus.lostandfound.model.ClaimStatus;
import com.campus.lostandfound.model.ItemStatus;
import com.campus.lostandfound.model.ItemType;
import com.campus.lostandfound.service.ClaimService;

@WebMvcTest(ClaimController.class)
@Import(SecurityConfig.class)
class ClaimControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClaimService claimService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private RequestPostProcessor student(long id) {
        return jwt().jwt(token -> token.subject(String.valueOf(id)).claim("role", "STUDENT"))
                .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"));
    }

    private ClaimResponse sampleClaim() {
        return new ClaimResponse(5L, 10L, "Keys", ItemStatus.CLAIM_PENDING, 2L, "Claimant",
                "It is mine", ClaimStatus.PENDING, LocalDateTime.now());
    }

    @Test
    void submitClaimReturns201() throws Exception {
        when(claimService.submitClaim(eq(2L), eq(10L), any(ClaimRequest.class))).thenReturn(sampleClaim());

        mockMvc.perform(post("/api/items/10/claims")
                        .with(student(2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"It is mine\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.itemName").value("Keys"));
    }

    @Test
    void blankMessageGives400() throws Exception {
        mockMvc.perform(post("/api/items/10/claims")
                        .with(student(2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.message").value("Message is required"));
    }

    @Test
    void submitClaimWithoutTokenGives401() throws Exception {
        mockMvc.perform(post("/api/items/10/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"mine\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateClaimGives409() throws Exception {
        when(claimService.submitClaim(eq(2L), eq(10L), any(ClaimRequest.class)))
                .thenThrow(new ConflictException("You already have a pending claim for this item"));

        mockMvc.perform(post("/api/items/10/claims")
                        .with(student(2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"again\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("You already have a pending claim for this item"));
    }

    @Test
    void myClaimsReturnsList() throws Exception {
        when(claimService.getMyClaims(2L)).thenReturn(List.of(sampleClaim()));

        mockMvc.perform(get("/api/claims/my").with(student(2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5));
    }

    @Test
    void foreignClaimGives403() throws Exception {
        when(claimService.getClaim(4L, 5L)).thenThrow(new ForbiddenException("You are not allowed to view this claim"));

        mockMvc.perform(get("/api/claims/5").with(student(4)))
                .andExpect(status().isForbidden());
    }

    @Test
    void markReturnedReturnsItem() throws Exception {
        when(claimService.markItemReturned(1L, 10L)).thenReturn(new ItemResponse(10L, "Keys", "Two keys", "Keys",
                ItemType.FOUND, "Canteen", LocalDate.now(), ItemStatus.RETURNED, null, 1L, "Finder", LocalDateTime.now()));

        mockMvc.perform(put("/api/items/10/returned").with(student(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"));
    }
}
