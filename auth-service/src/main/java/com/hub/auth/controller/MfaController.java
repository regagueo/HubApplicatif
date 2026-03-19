package com.hub.auth.controller;

import com.hub.auth.dto.*;
import com.hub.auth.security.UserPrincipal;
import com.hub.auth.service.MfaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/mfa")
@RequiredArgsConstructor
public class MfaController {

    private final MfaService mfaService;

    @PostMapping("/verify")
    public ResponseEntity<AuthResponse> verify(@Valid @RequestBody MfaVerifyRequest request,
                                              HttpServletRequest httpRequest) {
        AuthResponse response = mfaService.verifyMfaAndLogin(
                request.getTempToken(),
                request.getCode(),
                httpRequest
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/setup")
    public ResponseEntity<MfaSetupResponse> setup(@AuthenticationPrincipal UserPrincipal principal) {
        String secret = mfaService.generateSecret();
        String qrCodeUrl = mfaService.getQrCodeUrl(secret, principal.getUser().getUsername(), "Hub-ERP-CRM");
        return ResponseEntity.ok(MfaSetupResponse.builder()
                .secret(secret)
                .qrCodeUrl(qrCodeUrl)
                .build());
    }

    @PostMapping("/enable")
    public ResponseEntity<Void> enable(@AuthenticationPrincipal UserPrincipal principal,
                                      @RequestBody MfaEnableRequest request) {
        mfaService.enableMfa(principal.getUser().getId(), request.getSecret(), request.getCode());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/disable")
    public ResponseEntity<Void> disable(@AuthenticationPrincipal UserPrincipal principal) {
        mfaService.disableMfa(principal.getUser().getId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/enable-email")
    public ResponseEntity<Void> enableEmail(@AuthenticationPrincipal UserPrincipal principal) {
        mfaService.enableMfaByEmail(principal.getUser().getId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<Void> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        mfaService.resendOtp(request.getTempToken());
        return ResponseEntity.ok().build();
    }
}
