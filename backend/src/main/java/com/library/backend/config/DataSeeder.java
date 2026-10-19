package com.library.backend.config;

import com.library.backend.entity.Role;
import com.library.backend.entity.User;
import com.library.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@library.local}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Value("${app.admin.name:Head Librarian}")
    private String adminName;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByRole(Role.ADMIN)) {
            log.info("Admin user already exists. Skipping root admin seeding.");
            return;
        }
        if (adminPassword == null || adminPassword.isBlank()) {
            log.warn("No ADMIN exists and no admin password is set (app.admin.password / ADMIN_PASSWORD). "
                + "Set it and restart to create the first admin. Skipping seeding.");
            return;
        }
        log.info("No ADMIN found. Seeding root admin: {}", adminEmail);
        User admin = new User();
        admin.setName(adminName);
        admin.setEmail(User.normalizeEmail(adminEmail));
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        admin.setActive(true);
        userRepository.save(admin);
        log.info("Root admin created successfully.");
    }
}
