package com.lostfound.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import com.lostfound.exception.BadRequestException;

/** Verifies a Google ID token: valid Google signature, not expired, issued by Google for our GOOGLE_CLIENT_ID, verified email. */
@Component
public class GoogleTokenVerifier {

    /** Facts taken from a verified token. */
    public record GoogleProfile(String googleId, String email, String name) {
    }

    private static final Logger log = LoggerFactory.getLogger(GoogleTokenVerifier.class);
    private static final String GOOGLE_KEYS_URL = "https://www.googleapis.com/oauth2/v3/certs";
    private static final List<String> GOOGLE_ISSUERS = List.of("https://accounts.google.com", "accounts.google.com");

    private final JwtDecoder decoder; // null when GOOGLE_CLIENT_ID is not set

    @Autowired
    public GoogleTokenVerifier(@Value("${app.google.client-id}") String clientId) {
        this(buildDecoder(clientId));
    }

    /** For tests (fake decoder). */
    GoogleTokenVerifier(JwtDecoder decoder) {
        this.decoder = decoder;
    }

    public GoogleProfile verify(String idToken) {
        if (decoder == null) {
            throw new BadRequestException("Google sign-in is not configured on this server");
        }
        Jwt jwt;
        try {
            jwt = decoder.decode(idToken);
        } catch (JwtException e) {
            log.debug("Google token rejected: {}", e.getMessage());
            throw new BadCredentialsException("Invalid Google sign-in token");
        }
        if (!Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"))) {
            throw new BadCredentialsException("Your Google email address is not verified");
        }
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            throw new BadCredentialsException("Google did not provide an email address");
        }
        String name = jwt.getClaimAsString("name");
        if (name == null || name.isBlank()) {
            name = email.substring(0, email.indexOf('@') > 0 ? email.indexOf('@') : email.length());
        }
        return new GoogleProfile(jwt.getSubject(), email, name);
    }

    /** Issuer and audience rules (separate so they can be tested). */
    static OAuth2TokenValidator<Jwt> googleRules(String clientId) {
        return jwt -> {
            String issuer = jwt.getClaimAsString("iss");
            if (issuer == null || !GOOGLE_ISSUERS.contains(issuer)) {
                return OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "Token was not issued by Google", null));
            }
            List<String> audience = jwt.getAudience();
            if (audience == null || !audience.contains(clientId)) {
                return OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "Token was not issued for this application", null));
            }
            return OAuth2TokenValidatorResult.success();
        };
    }

    private static JwtDecoder buildDecoder(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            return null;
        }
        NimbusJwtDecoder nimbus = NimbusJwtDecoder.withJwkSetUri(GOOGLE_KEYS_URL).build();
        nimbus.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(),
                googleRules(clientId.trim())));
        return nimbus;
    }
}
