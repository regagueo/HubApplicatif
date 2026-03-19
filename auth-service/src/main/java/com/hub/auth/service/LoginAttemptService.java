package com.hub.auth.service;

import com.hub.auth.entity.LoginAttempt;
import com.hub.auth.repository.LoginAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final LoginAttemptRepository loginAttemptRepository;

    @Value("${app.security.max-login-attempts:5}")
    private int maxAttempts;

    @Value("${app.security.lockout-duration-minutes:15}")
    private int lockoutMinutes;

    @Transactional
    public void recordFailedAttempt(String identifier) {
        LoginAttempt attempt = loginAttemptRepository.findByIdentifier(identifier)
                .orElse(LoginAttempt.builder()
                        .identifier(identifier)
                        .attemptCount(0)
                        .build());

        attempt.setAttemptCount(attempt.getAttemptCount() + 1);
        if (attempt.getAttemptCount() >= maxAttempts) {
            attempt.setLockedUntil(LocalDateTime.now().plusMinutes(lockoutMinutes));
        }
        loginAttemptRepository.save(attempt);
    }

    @Transactional
    public void recordSuccess(String identifier) {
        loginAttemptRepository.findByIdentifier(identifier)
                .ifPresent(attempt -> {
                    attempt.setAttemptCount(0);
                    attempt.setLockedUntil(null);
                    loginAttemptRepository.save(attempt);
                });
    }

    public boolean isBlocked(String identifier) {
        return loginAttemptRepository.findByIdentifier(identifier)
                .map(attempt -> {
                    if (attempt.getLockedUntil() == null) return false;
                    if (LocalDateTime.now().isAfter(attempt.getLockedUntil())) {
                        attempt.setAttemptCount(0);
                        attempt.setLockedUntil(null);
                        loginAttemptRepository.save(attempt);
                        return false;
                    }
                    return true;
                })
                .orElse(false);
    }

    public int getRemainingAttempts(String identifier) {
        return loginAttemptRepository.findByIdentifier(identifier)
                .map(a -> Math.max(0, maxAttempts - a.getAttemptCount()))
                .orElse(maxAttempts);
    }

    public LocalDateTime getLockedUntil(String identifier) {
        return loginAttemptRepository.findByIdentifier(identifier)
                .map(LoginAttempt::getLockedUntil)
                .orElse(null);
    }
}
