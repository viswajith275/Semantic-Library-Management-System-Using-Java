package com.library.backend.controller;

import com.library.backend.dto.AuthDto.*;
import com.library.backend.entity.User;
import com.library.backend.security.SecurityUtils;
import com.library.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/admin/login")
    public ResponseEntity<AdminAuthResponse> adminLogin(@Valid @RequestBody AdminLoginRequest request) {
        return ResponseEntity.ok(authService.adminLogin(request));
    }

    @PostMapping("/admin/logout")
    public ResponseEntity<Void> adminLogout() {
        User admin = SecurityUtils.getCurrentUser();
        authService.adminLogout(admin);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/member/login")
    public ResponseEntity<MemberAuthResponse> memberLogin(@Valid @RequestBody MemberLoginRequest request) {
        return ResponseEntity.ok(authService.memberLogin(request));
    }
}
