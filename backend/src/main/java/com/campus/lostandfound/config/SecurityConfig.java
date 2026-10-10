package com.campus.lostandfound.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Who may call which URL.
 *
 * How a request is authenticated: the client sends "Authorization: Bearer <token>".
 * Spring's JWT filter verifies the signature and expiry (JwtDecoder) and, if valid,
 * builds the logged-in user, with authority ROLE_STUDENT or ROLE_ADMIN taken from
 * the token's "role" claim. The server keeps no session (STATELESS).
 */
@Configuration
public class SecurityConfig {

    /** BCrypt: slow, salted one-way hashing. Used to store and check passwords. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        JsonSecurityErrorHandler errorHandler = new JsonSecurityErrorHandler(objectMapper);

        http
            .csrf(csrf -> csrf.disable())              // not needed: we use tokens, not cookies
            .cors(Customizer.withDefaults())           // uses CorsConfig (for the React app)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/google", "/api/health").permitAll()
                // order matters: "/my" must be checked before the public "/{id}" rule
                .requestMatchers(HttpMethod.GET, "/api/items/my").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/items", "/api/items/{id}").permitAll()
                // pictures are public so an <img> tag (which cannot send a token) can show them;
                // uploading (POST /api/uploads) needs a login
                .requestMatchers(HttpMethod.GET, "/api/uploads/files/{filename}").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth -> oauth
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                .authenticationEntryPoint(errorHandler)
                .accessDeniedHandler(errorHandler))
            .exceptionHandling(handling -> handling
                .authenticationEntryPoint(errorHandler)
                .accessDeniedHandler(errorHandler));

        return http.build();
    }

    /** Turns the token's "role": "ADMIN" into the authority "ROLE_ADMIN" that hasRole("ADMIN") checks. */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("role");
        authorities.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
