package com.hub.auth.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class LoginAttemptServiceTest {

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Test
    void shouldBlockAfterMaxAttempts() {
        String identifier = "testuser_block";
        for (int i = 0; i < 5; i++) {
            loginAttemptService.recordFailedAttempt(identifier);
        }
        assertTrue(loginAttemptService.isBlocked(identifier));
    }

    @Test
    void shouldResetOnSuccess() {
        String identifier = "testuser_reset";
        loginAttemptService.recordFailedAttempt(identifier);
        loginAttemptService.recordFailedAttempt(identifier);
        loginAttemptService.recordSuccess(identifier);
        assertFalse(loginAttemptService.isBlocked(identifier));
    }
}
