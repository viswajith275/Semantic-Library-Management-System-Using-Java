package com.library.backend.security;

import com.library.backend.entity.Role;
import com.library.backend.entity.User;
import com.library.backend.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class StatelessAuthFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    public StatelessAuthFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        String codeHeader = request.getHeader("X-Auth-Code");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            if (!token.isBlank()) {
                userRepository.findByApiToken(token).ifPresent(user -> {
                    if (user.getRole() == Role.ADMIN
                            && Boolean.TRUE.equals(user.getActive())
                            && user.getTokenExpiresAt() != null
                            && user.getTokenExpiresAt().isAfter(LocalDateTime.now())) {
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                        );
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                });
            }
        } else if (codeHeader != null && !codeHeader.isBlank()) {
            String customCode = codeHeader.trim();
            userRepository.findByCustomCode(customCode).ifPresent(user -> {
                if (user.getRole() == Role.MEMBER && Boolean.TRUE.equals(user.getActive())) {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_MEMBER"))
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            });
        }

        filterChain.doFilter(request, response);
    }
}
