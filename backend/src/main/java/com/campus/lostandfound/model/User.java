package com.campus.lostandfound.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * A registered person. Mapped to the "users" table ("user" is a reserved word
 * in PostgreSQL, so we avoid it).
 *
 * Encapsulation: every field is private. Other classes can only read or change
 * data through the getters/setters below. There is deliberately no setter for
 * id or createdAt, because those must never change.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    // Only the BCrypt hash is stored, never the real password.
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // Google's permanent id for this person ("sub"). Null for email/password accounts.
    @Column(name = "google_id", unique = true)
    private String googleId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Required by JPA, which creates objects itself when reading from the database. */
    protected User() {
    }

    public User(String name, String email, String passwordHash, Role role) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    /** Constructor overloading: an account created through Google sign-in. */
    public User(String name, String email, String passwordHash, Role role, String googleId) {
        this(name, email, passwordHash, role);
        this.googleId = googleId;
    }

    /** Runs automatically just before the first INSERT. */
    @PrePersist
    private void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getGoogleId() {
        return googleId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
