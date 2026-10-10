package com.campus.lostandfound.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import com.campus.lostandfound.config.JwtConfig;
import com.campus.lostandfound.model.Role;
import com.campus.lostandfound.model.User;

/** Creates a token with the real encoder and reads it back with the real decoder. */
class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-that-is-long-enough-123456";

    private User fakeUser() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(5L);
        when(user.getRole()).thenReturn(Role.ADMIN);
        return user;
    }

    @Test
    void tokenContainsUserIdAndRoleAndIsAcceptedByDecoder() {
        JwtConfig config = new JwtConfig(SECRET);
        JwtService jwtService = new JwtService(config.jwtEncoder(), 60);

        String token = jwtService.generateToken(fakeUser());
        Jwt decoded = config.jwtDecoder().decode(token);

        assertEquals("5", decoded.getSubject());
        assertEquals("ADMIN", decoded.getClaimAsString("role"));
        assertEquals(3600, jwtService.getExpiresInSeconds());
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtService jwtService = new JwtService(new JwtConfig(SECRET).jwtEncoder(), 60);
        String token = jwtService.generateToken(fakeUser());

        JwtDecoder otherDecoder = new JwtConfig("a-completely-different-secret-value-9876543210").jwtDecoder();

        assertThrows(JwtException.class, () -> otherDecoder.decode(token));
    }

    @Test
    void shortSecretIsRefused() {
        assertThrows(IllegalStateException.class, () -> new JwtConfig("too-short"));
    }
}
