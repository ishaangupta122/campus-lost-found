package com.lostfound.service;

import java.util.UUID;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lostfound.dto.AuthPayloads.AuthResponse;
import com.lostfound.dto.AuthPayloads.GoogleLoginRequest;
import com.lostfound.dto.AuthPayloads.LoginRequest;
import com.lostfound.dto.AuthPayloads.RegisterRequest;
import com.lostfound.dto.UserResponse;
import com.lostfound.exception.ConflictException;
import com.lostfound.model.Role;
import com.lostfound.model.User;
import com.lostfound.repository.UserRepository;
import com.lostfound.service.GoogleTokenVerifier.GoogleProfile;

/** Registration, login and Google login. */
@Service
@Transactional
public class AuthService {

    // same message for unknown email and wrong password
    private static final String BAD_CREDENTIALS = "Invalid email or password";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleTokenVerifier googleTokenVerifier;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, GoogleTokenVerifier googleTokenVerifier) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.googleTokenVerifier = googleTokenVerifier;
    }

    public AuthResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        String hash = passwordEncoder.encode(request.password());
        User saved = userRepository.save(new User(request.name().trim(), email, hash, Role.STUDENT));
        return buildResponse(saved);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalize(request.email()))
                .orElseThrow(() -> new BadCredentialsException(BAD_CREDENTIALS));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException(BAD_CREDENTIALS);
        }
        return buildResponse(user);
    }

    /** Finds the user by Google id or creates a STUDENT. Refuses if the email already belongs to a password account (prevents account takeover). */
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        GoogleProfile profile = googleTokenVerifier.verify(request.idToken());
        User user = userRepository.findByGoogleId(profile.googleId())
                .orElseGet(() -> createGoogleUser(profile));
        return buildResponse(user);
    }

    private User createGoogleUser(GoogleProfile profile) {
        String email = normalize(profile.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException(
                    "An account with this email already exists. Log in with your email and password instead");
        }
        // Google users get a hash of a random value, so password login never works for them
        String unusableHash = passwordEncoder.encode(UUID.randomUUID().toString());
        String name = profile.name().trim();
        if (name.length() > 100) {
            name = name.substring(0, 100);
        }
        return userRepository.save(new User(name, email, unusableHash, Role.STUDENT, profile.googleId()));
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long userId) {
        return userRepository.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> new BadCredentialsException("Unknown user"));
    }

    private AuthResponse buildResponse(User user) {
        return new AuthResponse(jwtService.generateToken(user), "Bearer",
                jwtService.getExpiresInSeconds(), UserResponse.from(user));
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }
}
