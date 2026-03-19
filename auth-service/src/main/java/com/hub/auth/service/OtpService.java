package com.hub.auth.service;

import com.hub.auth.entity.OtpVerification;
import com.hub.auth.entity.User;
import com.hub.auth.repository.OtpVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Service OTP : génération, stockage en base, vérification.
 * Gère expiration (5 min) et limite à 5 tentatives.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_ATTEMPTS = 5;

    private final OtpVerificationRepository otpRepository;
    private final EmailService emailService;

    @Value("${app.mfa.otp-validity-minutes:5}")
    private int otpValidityMinutes;

    /**
     * Génère un OTP à 6 chiffres, le stocke en base et l'envoie par email.
     */
    @Transactional
    public String generateAndSend(User user) {
        otpRepository.deleteByUserId(user.getId());

        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(otpValidityMinutes);

        OtpVerification otpVerification = OtpVerification.builder()
                .userId(user.getId())
                .otp(otp)
                .expiresAt(expiresAt)
                .attemptCount(0)
                .build();
        otpRepository.save(otpVerification);

        emailService.sendOtpEmail(user.getEmail(), otp, user.getUsername());
        log.info("OTP généré pour l'utilisateur {}", user.getId());
        return otp;
    }

    /**
     * Vérifie le code OTP : correspondance, expiration, max 5 tentatives.
     */
    @Transactional
    public boolean verify(Long userId, String code) {
        OtpVerification otpVerification = otpRepository.findByUserId(userId)
                .orElse(null);

        if (otpVerification == null) {
            return false;
        }

        if (LocalDateTime.now().isAfter(otpVerification.getExpiresAt())) {
            otpRepository.delete(otpVerification);
            throw new RuntimeException("Code expiré. Veuillez vous reconnecter pour recevoir un nouveau code.");
        }

        if (otpVerification.getAttemptCount() >= MAX_ATTEMPTS) {
            otpRepository.delete(otpVerification);
            throw new RuntimeException("Nombre maximum de tentatives atteint. Veuillez vous reconnecter.");
        }

        otpVerification.setAttemptCount(otpVerification.getAttemptCount() + 1);
        otpRepository.save(otpVerification);

        if (otpVerification.getOtp().equals(code)) {
            otpRepository.delete(otpVerification);
            return true;
        }

        int remaining = MAX_ATTEMPTS - otpVerification.getAttemptCount();
        throw new RuntimeException("Code invalide. Tentatives restantes : " + remaining);
    }

    @Scheduled(cron = "${app.mfa.cleanup-cron:0 */5 * * * *}")
    @Transactional
    public void cleanupExpired() {
        otpRepository.deleteExpired(LocalDateTime.now());
    }
}
