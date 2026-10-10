package com.campus.lostandfound.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.campus.lostandfound.config.SecurityConfig;
import com.campus.lostandfound.dto.AuthResponse;
import com.campus.lostandfound.dto.GoogleLoginRequest;
import com.campus.lostandfound.dto.LoginRequest;
import com.campus.lostandfound.dto.RegisterRequest;
import com.campus.lostandfound.dto.UserResponse;
import com.campus.lostandfound.exception.UnauthorizedException;
import com.campus.lostandfound.model.Role;
import com.campus.lostandfound.service.AuthService;

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
                new UserResponse(1L, "Asha", "asha@campus.edu", Role.STUDENT));
    }

    @Test
    void registerReturns201WithToken() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(sampleAuthResponse());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Asha\",\"email\":\"asha@campus.edu\",\"password\":\"secret123\"}"))
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
                .thenThrow(new UnauthorizedException("Invalid email or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"asha@campus.edu\",\"password\":\"wrong\"}"))
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
                .thenReturn(new UserResponse(1L, "Asha", "asha@campus.edu", Role.STUDENT));

        mockMvc.perform(get("/api/auth/me").with(jwt().jwt(token -> token.subject("1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("asha@campus.edu"));
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

    @Test
    void googleLoginWithoutTokenFieldGives400() throws Exception {
        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.idToken").exists());
    }

    @Test
    void invalidGoogleTokenGives401() throws Exception {
        when(authService.loginWithGoogle(any(GoogleLoginRequest.class)))
                .thenThrow(new UnauthorizedException("Invalid Google sign-in token"));

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"forged\"}"))
                .andExpect(status().isUnauthorized());
    }
}
