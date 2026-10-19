package com.library.backend.service;

import com.library.backend.dto.AuthDto.*;
import com.library.backend.entity.Role;
import com.library.backend.entity.User;
import com.library.backend.exception.BadRequestException;
import com.library.backend.exception.ConflictException;
import com.library.backend.exception.UnauthorizedException;
import com.library.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCKOUT = Duration.ofMinutes(15);
    private static final int MAX_THROTTLE_ENTRIES = 10_000;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Per-process failed member-login counter. Single-instance deployment, so no shared store needed.
    private final Map<String, Attempts> failedLogins = new ConcurrentHashMap<>();

    private record Attempts(int count, Instant windowStart) {}

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AdminAuthResponse adminLogin(AdminLoginRequest request) {
        String email = User.normalizeEmail(request.email());
        User user = userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new UnauthorizedException("Invalid admin credentials"));

        if (user.getRole() != Role.ADMIN || !Boolean.TRUE.equals(user.getActive())) {
            throw new UnauthorizedException("Invalid admin credentials or account inactive");
        }

        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid admin credentials");
        }

        // Always issue a fresh token; never reuse the previous one.
        LocalDateTime now = LocalDateTime.now();
        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = now.plusHours(24);
        user.setApiToken(token);
        user.setTokenExpiresAt(expiresAt);
        userRepository.save(user);

        return new AdminAuthResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            Role.ADMIN.name(),
            token,
            expiresAt
        );
    }

    @Transactional
    public void adminLogout(User admin) {
        admin.setApiToken(null);
        admin.setTokenExpiresAt(null);
        userRepository.save(admin);
    }

    @Transactional(readOnly = true)
    public MemberAuthResponse memberLogin(MemberLoginRequest request) {
        String code = request.customCode().trim();

        if (isLockedOut(code)) {
            throw new UnauthorizedException("Too many failed attempts. Try again later.");
        }

        User user = userRepository.findByCustomCode(code).orElse(null);
        if (user == null || user.getRole() != Role.MEMBER || !Boolean.TRUE.equals(user.getActive())) {
            recordFailure(code);
            throw new UnauthorizedException("Member account is inactive or invalid");
        }

        clearFailures(code);

        return new MemberAuthResponse(
            user.getId(),
            user.getName(),
            Role.MEMBER.name(),
            user.getCustomCode()
        );
    }

    @Transactional
    public AdminAuthResponse createAdmin(CreateAdminRequest request) {
        String email = User.normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("User with email " + email + " already exists");
        }

        if (request.password() == null || request.password().isBlank()) {
            throw new BadRequestException("Password cannot be blank");
        }

        User admin = new User();
        admin.setName(request.name().trim());
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(request.password()));
        admin.setRole(Role.ADMIN);
        admin.setActive(true);

        User saved = userRepository.save(admin);

        return new AdminAuthResponse(
            saved.getId(),
            saved.getName(),
            saved.getEmail(),
            Role.ADMIN.name(),
            null,
            null
        );
    }

    private boolean isLockedOut(String code) {
        Attempts attempts = failedLogins.get(code);
        if (attempts == null || attempts.count() < MAX_FAILED_ATTEMPTS) {
            return false;
        }
        if (Duration.between(attempts.windowStart(), Instant.now()).compareTo(LOCKOUT) >= 0) {
            failedLogins.remove(code);
            return false;
        }
        return true;
    }

    private void recordFailure(String code) {
        // shortcut: crude cap on the throttle map; clear it rather than track eviction. Fine for one instance.
        if (failedLogins.size() >= MAX_THROTTLE_ENTRIES) {
            failedLogins.clear();
        }
        failedLogins.merge(code, new Attempts(1, Instant.now()),
            (old, fresh) -> Duration.between(old.windowStart(), Instant.now()).compareTo(LOCKOUT) >= 0
                ? fresh
                : new Attempts(old.count() + 1, old.windowStart()));
    }

    private void clearFailures(String code) {
        failedLogins.remove(code);
    }
}
