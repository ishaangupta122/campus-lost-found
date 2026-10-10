package com.campus.lostandfound.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
import com.campus.lostandfound.dto.ItemCreateRequest;
import com.campus.lostandfound.dto.ItemResponse;
import com.campus.lostandfound.exception.ForbiddenException;
import com.campus.lostandfound.exception.ResourceNotFoundException;
import com.campus.lostandfound.model.ItemStatus;
import com.campus.lostandfound.model.ItemType;
import com.campus.lostandfound.service.ItemService;

/** Tests the web layer with security on. "Logged in" is simulated with a fake JWT. */
@WebMvcTest(ItemController.class)
@Import(SecurityConfig.class)
class ItemControllerTest {

    private static final String VALID_BODY = """
            {"itemName":"Backpack","description":"Blue backpack","category":"Bags",
             "type":"LOST","location":"Library"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItemService itemService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private RequestPostProcessor loggedIn(long userId, String role) {
        return jwt().jwt(token -> token.subject(String.valueOf(userId)).claim("role", role))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private ItemResponse sampleResponse() {
        return new ItemResponse(1L, "Backpack", "Blue backpack", "Bags", ItemType.LOST, "Library",
                LocalDate.of(2026, 10, 1), ItemStatus.OPEN, null, 1L, "Alice", LocalDateTime.now());
    }

    @Test
    void createReturns201WithJson() throws Exception {
        when(itemService.createItem(eq(1L), any(ItemCreateRequest.class))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/items")
                        .with(loggedIn(1, "STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.itemName").value("Backpack"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.reporterName").value("Alice"));
    }

    @Test
    void blankItemNameGives400WithFieldError() throws Exception {
        String body = """
                {"itemName":"","description":"x","category":"Bags","type":"LOST","location":"Library"}
                """;

        mockMvc.perform(post("/api/items")
                        .with(loggedIn(1, "STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.itemName").value("Item name is required"));
    }

    @Test
    void createWithoutTokenGives401() throws Exception {
        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void myItemsWithoutTokenGives401() throws Exception {
        mockMvc.perform(get("/api/items/my"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicListNeedsNoToken() throws Exception {
        mockMvc.perform(get("/api/items"))
                .andExpect(status().isOk());
    }

    @Test
    void serviceForbiddenBecomes403() throws Exception {
        when(itemService.createItem(eq(3L), any(ItemCreateRequest.class)))
                .thenThrow(new ForbiddenException("Only students can create reports"));

        mockMvc.perform(post("/api/items")
                        .with(loggedIn(3, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only students can create reports"));
    }

    @Test
    void missingItemGives404() throws Exception {
        when(itemService.getItem(99L)).thenThrow(new ResourceNotFoundException("Item 99 not found"));

        mockMvc.perform(get("/api/items/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Item 99 not found"));
    }

    @Test
    void invalidTypeFilterGives400() throws Exception {
        mockMvc.perform(get("/api/items").param("type", "BANANA"))
                .andExpect(status().isBadRequest());
    }
}
