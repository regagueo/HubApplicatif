package com.hub.auth.service;

import com.hub.auth.dto.*;
import com.hub.auth.entity.Role;
import com.hub.auth.entity.User;
import com.hub.auth.repository.UserRepository;
import com.hub.auth.security.JwtService;
import com.hub.auth.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final AuditService auditService;
    private final LoginAttemptService loginAttemptService;
    private final OtpService otpService;

    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists: " + request.getEmail());
        }

        Set<Role> roles = new HashSet<>();
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            for (String roleStr : request.getRoles()) {
                try {
                    roles.add(Role.valueOf(roleStr.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("Invalid role: " + roleStr);
                }
            }
        }
        if (roles.isEmpty()) {
            roles.add(Role.EMPLOYEE);
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .roles(roles)
                .enabled(true)
                .build();

        user = userRepository.save(user);

        auditService.log("REGISTER", "USER", user.getId().toString(),
                "New user registered: " + user.getUsername(), user, httpRequest);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationMs() / 1000)
                .user(mapToUserResponse(user))
                .build();
    }

    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String identifier = request.getUsernameOrEmail();

        if (loginAttemptService.isBlocked(identifier)) {
            LocalDateTime lockedUntil = loginAttemptService.getLockedUntil(identifier);
            throw new RuntimeException("Compte bloqué. Réessayez après " + lockedUntil);
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(identifier, request.getPassword())
            );

            loginAttemptService.recordSuccess(identifier);

            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            User user = principal.getUser();

            auditService.log("LOGIN", "AUTH", user.getId().toString(),
                    "User logged in successfully", user, httpRequest);

            if (user.isMfaEnabled() && user.getEmail() != null && !user.getEmail().isBlank()) {
                otpService.generateAndSend(user);
                String tempToken = jwtService.generateMfaTempToken(user);
                return AuthResponse.builder()
                        .requiresMfa(true)
                        .tempToken(tempToken)
                        .user(mapToUserResponse(user))
                        .build();
            }

            String accessToken = jwtService.generateAccessToken(authentication);
            String refreshToken = jwtService.generateRefreshToken(user);

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtService.getAccessTokenExpirationMs() / 1000)
                    .user(mapToUserResponse(user))
                    .build();
        } catch (BadCredentialsException e) {
            loginAttemptService.recordFailedAttempt(identifier);
            int remaining = loginAttemptService.getRemainingAttempts(identifier);
            if (remaining <= 0) {
                throw new RuntimeException("Compte bloqué après trop de tentatives. Réessayez dans 15 minutes.");
            }
            throw new RuntimeException("Identifiants incorrects. Tentatives restantes: " + remaining);
        }
    }

    public List<UserPeekDto> getUsersForChat() {
        return userRepository.findAll().stream()
                .map(u -> UserPeekDto.builder().id(u.getId()).username(u.getUsername()).build())
                .collect(Collectors.toList());
    }

    public Page<UserResponse> getUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::mapToUserResponse);
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
        return mapToUserResponse(user);
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .enabled(user.isEnabled())
                .mfaEnabled(user.isMfaEnabled())
                .roles(user.getRoles())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
