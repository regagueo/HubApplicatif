package com.hub.auth.service;

import com.hub.auth.dto.AuthResponse;
import com.hub.auth.dto.UserResponse;
import com.hub.auth.entity.User;
import com.hub.auth.repository.UserRepository;
import com.hub.auth.security.JwtService;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MfaService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final OtpService otpService;

    private static final int SECRET_SIZE = 32;

    public AuthResponse verifyMfaAndLogin(String tempToken, String code, HttpServletRequest httpRequest) {
        if (!jwtService.validateToken(tempToken)) {
            throw new RuntimeException("Session expirée. Veuillez vous reconnecter.");
        }

        Long userId = jwtService.getUserIdFromToken(tempToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (!user.isMfaEnabled()) {
            throw new RuntimeException("MFA non activé pour ce compte");
        }

        // MFA par email
        if (user.getMfaSecret() == null || user.getMfaSecret().isEmpty()) {
            if (!otpService.verify(userId, code)) {
                throw new RuntimeException("Code invalide");
            }
        } else {
            // MFA TOTP (authenticator)
            CodeVerifier verifier = new DefaultCodeVerifier(new DefaultCodeGenerator(), new SystemTimeProvider());
            if (!verifier.isValidCode(user.getMfaSecret(), code)) {
                throw new RuntimeException("Code invalide");
            }
        }

        auditService.log("LOGIN", "AUTH", user.getId().toString(),
                "User logged in with MFA", user, httpRequest);

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

    public String generateSecret() {
        SecretGenerator generator = new DefaultSecretGenerator(SECRET_SIZE);
        return generator.generate();
    }

    public String getQrCodeUrl(String secret, String username, String issuer) {
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s",
                issuer, username, secret, issuer);
    }

    public boolean verifyCode(String secret, String code) {
        CodeVerifier verifier = new DefaultCodeVerifier(new DefaultCodeGenerator(), new SystemTimeProvider());
        return verifier.isValidCode(secret, code);
    }

    @Transactional
    public void enableMfaByEmail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new RuntimeException("Un email est requis pour activer la validation en deux étapes");
        }
        user.setMfaEnabled(true);
        user.setMfaSecret(null);
        userRepository.save(user);
    }

    @Transactional
    public void enableMfa(Long userId, String secret, String verificationCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (!verifyCode(secret, verificationCode)) {
            throw new RuntimeException("Code de vérification invalide");
        }

        user.setMfaEnabled(true);
        user.setMfaSecret(secret);
        userRepository.save(user);
    }

    public void resendOtp(String tempToken) {
        if (!jwtService.validateToken(tempToken)) {
            throw new RuntimeException("Session expirée. Veuillez vous reconnecter.");
        }
        Long userId = jwtService.getUserIdFromToken(tempToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        if (!user.isMfaEnabled()) throw new RuntimeException("MFA non activé");
        otpService.generateAndSend(user);
    }

    @Transactional
    public void disableMfa(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        userRepository.save(user);
    }
}
