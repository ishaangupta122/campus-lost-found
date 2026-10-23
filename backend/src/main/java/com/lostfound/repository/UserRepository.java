package com.lostfound.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lostfound.model.User;

/** JpaRepository supplies save/find/delete; Spring Data builds the SQL from the method names. */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByGoogleId(String googleId);
}
