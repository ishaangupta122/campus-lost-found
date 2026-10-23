package com.lostfound.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.lostfound.model.Role;
import com.lostfound.model.User;
import com.lostfound.repository.UserRepository;

/** Creates the first admin from ADMIN_* in .env at startup, unless that account already exists. */
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
        String email = adminEmail.trim().toLowerCase();
        if (email.isEmpty() || adminPassword.isEmpty()) {
            log.info("Admin not created: ADMIN_EMAIL or ADMIN_PASSWORD is not set");
        } else if (adminPassword.length() < MIN_PASSWORD_LENGTH) {
            log.warn("Admin not created: ADMIN_PASSWORD must be at least {} characters", MIN_PASSWORD_LENGTH);
        } else if (userRepository.existsByEmail(email)) {
            log.info("Admin {} already exists", email);
        } else {
            userRepository.save(new User(adminName.trim(), email, passwordEncoder.encode(adminPassword), Role.ADMIN));
            log.info("Admin created: {}", email);
        }
    }
}
