package com.campus.lostandfound.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.campus.lostandfound.config.SecurityConfig;
import com.campus.lostandfound.exception.BadRequestException;
import com.campus.lostandfound.exception.ResourceNotFoundException;
import com.campus.lostandfound.service.ImageStorageService;

/** Uploading needs a login; viewing a stored picture is public (an <img> tag cannot send a token). */
@WebMvcTest(UploadController.class)
@Import(SecurityConfig.class)
class UploadControllerTest {

    private static final String STORED = ImageStorageService.URL_PREFIX + "11111111-1111-1111-1111-111111111111.png";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImageStorageService imageStorageService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private RequestPostProcessor student() {
        return jwt().jwt(token -> token.subject("2").claim("role", "STUDENT"))
                .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"));
    }

    private MockMultipartFile file() {
        return new MockMultipartFile("file", "a.png", "image/png", new byte[] {1, 2, 3});
    }

    @Test
    void uploadNeedsALogin() throws Exception {
        mockMvc.perform(multipart("/api/uploads").file(file())).andExpect(status().isUnauthorized());
    }

    @Test
    void loggedInUserGetsTheImagePath() throws Exception {
        when(imageStorageService.store(any())).thenReturn(STORED);

        mockMvc.perform(multipart("/api/uploads").file(file()).with(student()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imagePath").value(STORED));
    }

    @Test
    void invalidImageIs400() throws Exception {
        when(imageStorageService.store(any())).thenThrow(new BadRequestException("Only JPG, PNG, GIF or WebP images are allowed"));

        mockMvc.perform(multipart("/api/uploads").file(file()).with(student()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only JPG, PNG, GIF or WebP images are allowed"));
    }

    @Test
    void missingFileIs400() throws Exception {
        mockMvc.perform(multipart("/api/uploads").with(student())).andExpect(status().isBadRequest());
    }

    @Test
    void anyoneCanViewAStoredPicture() throws Exception {
        when(imageStorageService.load("pic.png")).thenReturn(new ByteArrayResource(new byte[] {1, 2, 3}));

        mockMvc.perform(get("/api/uploads/files/pic.png"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));
    }

    @Test
    void unknownPictureIs404() throws Exception {
        when(imageStorageService.load("missing.png")).thenThrow(new ResourceNotFoundException("Image not found"));

        mockMvc.perform(get("/api/uploads/files/missing.png")).andExpect(status().isNotFound());
    }
}
