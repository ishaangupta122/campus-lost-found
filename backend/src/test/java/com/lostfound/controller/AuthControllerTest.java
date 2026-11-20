package com.lostfound.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.lostfound.config.SecurityConfig;
import com.lostfound.dto.AuthPayloads.AuthResponse;
import com.lostfound.dto.AuthPayloads.GoogleLoginRequest;
import com.lostfound.dto.AuthPayloads.LoginRequest;
import com.lostfound.dto.AuthPayloads.RegisterRequest;
import com.lostfound.dto.UserResponse;
import com.lostfound.model.Role;
import com.lostfound.service.AuthService;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    // SecurityConfig needs a JwtDecoder; the real one lives in JwtConfig, which this slice does not load.
    @MockitoBean
    private JwtDecoder jwtDecoder;

    private AuthResponse sampleAuthResponse() {
        return new AuthResponse("fake.jwt.token", "Bearer", 3600,
                new UserResponse(1L, "Asha", "asha@thapar.edu", Role.STUDENT, false, LocalDateTime.now()));
    }

    @Test
    void registerReturns201WithToken() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(sampleAuthResponse());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Asha\",\"email\":\"asha@thapar.edu\",\"password\":\"secret123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("fake.jwt.token"))
                .andExpect(jsonPath("$.user.role").value("STUDENT"));
    }

    @Test
    void registerWithBadEmailAndShortPasswordGives400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Asha\",\"email\":\"not-an-email\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void wrongCredentialsGive401() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"asha@thapar.edu\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void meWithoutTokenGives401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void meWithTokenReturnsCurrentUser() throws Exception {
        when(authService.getCurrentUser(1L))
                .thenReturn(new UserResponse(1L, "Asha", "asha@thapar.edu", Role.STUDENT, false, LocalDateTime.now()));

        mockMvc.perform(get("/api/auth/me").with(jwt().jwt(token -> token.subject("1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("asha@thapar.edu"));
    }

    @Test
    void googleLoginReturnsToken() throws Exception {
        when(authService.loginWithGoogle(any(GoogleLoginRequest.class))).thenReturn(sampleAuthResponse());

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"some-google-id-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("fake.jwt.token"));
    }
}
