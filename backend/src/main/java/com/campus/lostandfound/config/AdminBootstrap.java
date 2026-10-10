package com.campus.lostandfound.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.campus.lostandfound.model.Role;
import com.campus.lostandfound.model.User;
import com.campus.lostandfound.repository.UserRepository;

/**
 * Creates the first admin account when the app starts, from ADMIN_NAME,
 * ADMIN_EMAIL and ADMIN_PASSWORD in .env. Students can never become admins
 * through the API, so this is the only way to get the first one.
 *
 * - If ADMIN_EMAIL or ADMIN_PASSWORD is empty, nothing happens.
 * - If a user with that email already exists, nothing is changed.
 *
 * CommandLineRunner is an interface; our run() method overrides (implements)
 * its abstract method, and Spring calls it once after startup.
 */
@Component
public class AdminBootstrap implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminName;
    private final String adminEmail;
    private final String adminPassword;

    public AdminBootstrap(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          @Value("${app.admin.name}") String adminName,
                          @Value("${app.admin.email}") String adminEmail,
                          @Value("${app.admin.password}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminName = adminName;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            log.info("No ADMIN_EMAIL / ADMIN_PASSWORD set: skipping admin creation");
            return;
        }
        if (adminPassword.length() < MIN_PASSWORD_LENGTH) {
            log.warn("ADMIN_PASSWORD is shorter than {} characters: admin not created", MIN_PASSWORD_LENGTH);
            return;
        }
        String email = adminEmail.trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            log.info("Admin account {} already exists", email);
            return;
        }
        userRepository.save(new User(adminName.trim(), email, passwordEncoder.encode(adminPassword), Role.ADMIN));
        log.info("Admin account created for {}", email);
    }
}
