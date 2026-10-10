package com.campus.lostandfound.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import com.campus.lostandfound.dto.AuthResponse;
import com.campus.lostandfound.dto.GoogleLoginRequest;
import com.campus.lostandfound.dto.GoogleProfile;
import com.campus.lostandfound.dto.LoginRequest;
import com.campus.lostandfound.dto.RegisterRequest;
import com.campus.lostandfound.dto.UserResponse;
import com.campus.lostandfound.exception.ConflictException;
import com.campus.lostandfound.exception.UnauthorizedException;
import com.campus.lostandfound.model.Role;
import com.campus.lostandfound.model.User;
import com.campus.lostandfound.repository.UserRepository;

/** Registration, login and "who am I". */
@Service
@Transactional
public class AuthService {

    // The same message for "no such email" and "wrong password", so attackers learn nothing.
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
                .orElseThrow(() -> new UnauthorizedException(BAD_CREDENTIALS));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException(BAD_CREDENTIALS);
        }
        return buildResponse(user);
    }

    /**
     * "Sign in with Google": the frontend sends Google's ID token. After it is verified we
     * find the user by Google id, or create a STUDENT account on first sign-in.
     * If the email already belongs to a password account we refuse instead of merging,
     * so nobody can take over an account by registering someone else's email first.
     */
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
        // Google users have no password. We store the hash of a random value nobody knows,
        // so password login can never succeed for them. (BCrypt accepts at most 72 bytes;
        // one random UUID is 36 characters, with about 122 bits of randomness.)
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
                .orElseThrow(() -> new UnauthorizedException("Unknown user"));
    }

    private AuthResponse buildResponse(User user) {
        return new AuthResponse(jwtService.generateToken(user), "Bearer",
                jwtService.getExpiresInSeconds(), UserResponse.from(user));
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }
}
