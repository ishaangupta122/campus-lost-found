package com.lostfound.controller;

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

import com.lostfound.config.SecurityConfig;
import com.lostfound.dto.ClaimRequests.ClaimRequest;
import com.lostfound.dto.ClaimResponse;
import com.lostfound.dto.ItemResponse;
import com.lostfound.model.ClaimStatus;
import com.lostfound.model.ItemStatus;
import com.lostfound.model.ItemType;
import com.lostfound.service.ClaimService;

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
    void myClaimsReturnsList() throws Exception {
        when(claimService.getMyClaims(2L)).thenReturn(List.of(sampleClaim()));

        mockMvc.perform(get("/api/claims/my").with(student(2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5));
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
