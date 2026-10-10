package com.campus.lostandfound.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.campus.lostandfound.dto.AuthResponse;
import com.campus.lostandfound.dto.GoogleLoginRequest;
import com.campus.lostandfound.dto.GoogleProfile;
import com.campus.lostandfound.dto.LoginRequest;
import com.campus.lostandfound.dto.RegisterRequest;
import com.campus.lostandfound.exception.ConflictException;
import com.campus.lostandfound.exception.UnauthorizedException;
import com.campus.lostandfound.model.Role;
import com.campus.lostandfound.model.User;
import com.campus.lostandfound.repository.UserRepository;

/** Uses a real BCrypt encoder (fast enough) and mock repository/token service. */
class AuthServiceTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private UserRepository userRepository;
    private JwtService jwtService;
    private GoogleTokenVerifier googleTokenVerifier;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        jwtService = mock(JwtService.class);
        googleTokenVerifier = mock(GoogleTokenVerifier.class);
        authService = new AuthService(userRepository, passwordEncoder, jwtService, googleTokenVerifier);

        when(jwtService.generateToken(any(User.class))).thenReturn("fake.jwt.token");
        when(jwtService.getExpiresInSeconds()).thenReturn(3600L);
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void registerCreatesStudentWithHashedPasswordAndNormalizedEmail() {
        when(userRepository.existsByEmail("asha@campus.edu")).thenReturn(false);

        AuthResponse response = authService.register(
                new RegisterRequest(" Asha ", "  Asha@Campus.EDU ", "secret123"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        User user = saved.getValue();
        assertEquals(Role.STUDENT, user.getRole());
        assertEquals("asha@campus.edu", user.getEmail());
        assertEquals("Asha", user.getName());
        assertNotEquals("secret123", user.getPasswordHash());       // never stored in plain text
        assertEquals(true, passwordEncoder.matches("secret123", user.getPasswordHash()));
        assertEquals("fake.jwt.token", response.token());
        assertEquals("Bearer", response.tokenType());
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmail("asha@campus.edu")).thenReturn(true);

        assertThrows(ConflictException.class, () ->
                authService.register(new RegisterRequest("Asha", "asha@campus.edu", "secret123")));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginSucceedsWithCorrectPassword() {
        User user = new User("Asha", "asha@campus.edu", passwordEncoder.encode("secret123"), Role.STUDENT);
        when(userRepository.findByEmail("asha@campus.edu")).thenReturn(Optional.of(user));

        AuthResponse response = authService.login(new LoginRequest("ASHA@campus.edu", "secret123"));

        assertEquals("fake.jwt.token", response.token());
        assertEquals("asha@campus.edu", response.user().email());
    }

    @Test
    void loginFailsWithWrongPassword() {
        User user = new User("Asha", "asha@campus.edu", passwordEncoder.encode("secret123"), Role.STUDENT);
        when(userRepository.findByEmail("asha@campus.edu")).thenReturn(Optional.of(user));

        assertThrows(UnauthorizedException.class, () ->
                authService.login(new LoginRequest("asha@campus.edu", "wrong-password")));
    }

    @Test
    void loginFailsForUnknownEmail() {
        when(userRepository.findByEmail("nobody@campus.edu")).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () ->
                authService.login(new LoginRequest("nobody@campus.edu", "secret123")));
    }

    // ----- Google sign-in -----

    @Test
    void firstGoogleSignInCreatesStudentWithUnusablePassword() {
        when(googleTokenVerifier.verify("id-token"))
                .thenReturn(new GoogleProfile("sub-1", "Asha@Campus.edu", "Asha Rao"));
        when(userRepository.findByGoogleId("sub-1")).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("asha@campus.edu")).thenReturn(false);

        AuthResponse response = authService.loginWithGoogle(new GoogleLoginRequest("id-token"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        User user = saved.getValue();
        assertEquals(Role.STUDENT, user.getRole());
        assertEquals("asha@campus.edu", user.getEmail());
        assertEquals("sub-1", user.getGoogleId());
        assertEquals(false, passwordEncoder.matches("", user.getPasswordHash()));
        assertEquals("fake.jwt.token", response.token());
    }

    @Test
    void returningGoogleUserIsNotCreatedAgain() {
        User existing = new User("Asha", "asha@campus.edu", "hash", Role.STUDENT, "sub-1");
        when(googleTokenVerifier.verify("id-token"))
                .thenReturn(new GoogleProfile("sub-1", "asha@campus.edu", "Asha"));
        when(userRepository.findByGoogleId("sub-1")).thenReturn(Optional.of(existing));

        AuthResponse response = authService.loginWithGoogle(new GoogleLoginRequest("id-token"));

        assertEquals("asha@campus.edu", response.user().email());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void googleSignInIsRefusedWhenEmailBelongsToPasswordAccount() {
        when(googleTokenVerifier.verify("id-token"))
                .thenReturn(new GoogleProfile("sub-2", "asha@campus.edu", "Asha"));
        when(userRepository.findByGoogleId("sub-2")).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("asha@campus.edu")).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> authService.loginWithGoogle(new GoogleLoginRequest("id-token")));
        verify(userRepository, never()).save(any(User.class));
    }
}
