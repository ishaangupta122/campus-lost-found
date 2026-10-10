package com.campus.lostandfound.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import com.campus.lostandfound.dto.GoogleProfile;
import com.campus.lostandfound.exception.BadRequestException;
import com.campus.lostandfound.exception.UnauthorizedException;

/** No network: a fake decoder stands in for Google's signature check. */
class GoogleTokenVerifierTest {

    private static final String CLIENT_ID = "my-client-id.apps.googleusercontent.com";

    private Jwt.Builder token() {
        return Jwt.withTokenValue("t")
                .header("alg", "RS256")
                .subject("google-sub-123")
                .claim("iss", "https://accounts.google.com")
                .claim("aud", List.of(CLIENT_ID))
                .claim("email", "asha@campus.edu")
                .claim("email_verified", true)
                .claim("name", "Asha Rao");
    }

    private GoogleTokenVerifier verifierReturning(Jwt jwt) {
        JwtDecoder decoder = mock(JwtDecoder.class);
        when(decoder.decode("good-token")).thenReturn(jwt);
        when(decoder.decode("bad-token")).thenThrow(new BadJwtException("bad signature"));
        return new GoogleTokenVerifier(decoder);
    }

    @Test
    void validTokenGivesProfile() {
        GoogleProfile profile = verifierReturning(token().build()).verify("good-token");

        assertEquals("google-sub-123", profile.googleId());
        assertEquals("asha@campus.edu", profile.email());
        assertEquals("Asha Rao", profile.name());
    }

    @Test
    void missingNameFallsBackToEmailPrefix() {
        Jwt jwt = token().claims(claims -> claims.remove("name")).build();

        assertEquals("asha", verifierReturning(jwt).verify("good-token").name());
    }

    @Test
    void invalidSignatureIsUnauthorized() {
        assertThrows(UnauthorizedException.class,
                () -> verifierReturning(token().build()).verify("bad-token"));
    }

    @Test
    void unverifiedEmailIsUnauthorized() {
        Jwt jwt = token().claim("email_verified", false).build();

        assertThrows(UnauthorizedException.class, () -> verifierReturning(jwt).verify("good-token"));
    }

    @Test
    void notConfiguredGivesBadRequest() {
        GoogleTokenVerifier notConfigured = new GoogleTokenVerifier("");

        assertThrows(BadRequestException.class, () -> notConfigured.verify("anything"));
    }

    @Test
    void rulesAcceptGoogleIssuerAndOurAudience() {
        assertFalse(GoogleTokenVerifier.googleRules(CLIENT_ID).validate(token().build()).hasErrors());
        assertFalse(GoogleTokenVerifier.googleRules(CLIENT_ID)
                .validate(token().claim("iss", "accounts.google.com").build()).hasErrors());
    }

    @Test
    void rulesRejectWrongAudienceAndWrongIssuer() {
        assertTrue(GoogleTokenVerifier.googleRules(CLIENT_ID)
                .validate(token().claim("aud", List.of("someone-elses-app")).build()).hasErrors());
        assertTrue(GoogleTokenVerifier.googleRules(CLIENT_ID)
                .validate(token().claim("iss", "https://evil.example.com").build()).hasErrors());
    }
}
