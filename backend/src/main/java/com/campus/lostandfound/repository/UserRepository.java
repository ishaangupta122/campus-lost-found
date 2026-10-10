package com.campus.lostandfound.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.campus.lostandfound.model.User;

/**
 * Extending JpaRepository gives save, findById, findAll, delete, count... for
 * free. The two methods below have no code: Spring Data reads the method name
 * and writes the SQL query itself ("derived queries").
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByGoogleId(String googleId);
}
