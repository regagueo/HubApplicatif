package com.hub.parametres.controller;

import com.hub.parametres.dto.*;
import com.hub.parametres.service.ParametresService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/parametres", "/parametres"})
@RequiredArgsConstructor
public class ParametresController {

    private final ParametresService parametresService;

    @GetMapping("/me/profile")
    public ResponseEntity<ProfilDto> getMyProfil() {
        Long userId = parametresService.getCurrentUser().getUserId();
        return ResponseEntity.ok(parametresService.getProfil(userId));
    }

    @PutMapping("/me/profile")
    public ResponseEntity<ProfilDto> updateMyProfil(@Valid @RequestBody ProfilDto dto) {
        Long userId = parametresService.getCurrentUser().getUserId();
        return ResponseEntity.ok(parametresService.updateProfil(userId, dto));
    }

    @GetMapping("/me/preferences")
    public ResponseEntity<PreferencesDto> getMyPreferences() {
        Long userId = parametresService.getCurrentUser().getUserId();
        return ResponseEntity.ok(parametresService.getPreferences(userId));
    }

    @PutMapping("/me/preferences")
    public ResponseEntity<PreferencesDto> updateMyPreferences(@Valid @RequestBody PreferencesDto dto) {
        Long userId = parametresService.getCurrentUser().getUserId();
        return ResponseEntity.ok(parametresService.updatePreferences(userId, dto));
    }

    @GetMapping("/me/security")
    public ResponseEntity<SecuriteDto> getMySecurity() {
        Long userId = parametresService.getCurrentUser().getUserId();
        return ResponseEntity.ok(parametresService.getSecurite(userId));
    }

    @PutMapping("/me/security")
    public ResponseEntity<SecuriteDto> updateMySecurity(@Valid @RequestBody SecuriteDto dto) {
        Long userId = parametresService.getCurrentUser().getUserId();
        return ResponseEntity.ok(parametresService.updateSecurite(userId, dto));
    }

    @GetMapping("/me/notifications")
    public ResponseEntity<NotificationsDto> getMyNotifications() {
        Long userId = parametresService.getCurrentUser().getUserId();
        return ResponseEntity.ok(parametresService.getNotifications(userId));
    }

    @PutMapping("/me/notifications")
    public ResponseEntity<NotificationsDto> updateMyNotifications(@Valid @RequestBody NotificationsDto dto) {
        Long userId = parametresService.getCurrentUser().getUserId();
        return ResponseEntity.ok(parametresService.updateNotifications(userId, dto));
    }

    @GetMapping("/{userId}/profile")
    public ResponseEntity<ProfilDto> getProfil(@PathVariable Long userId) {
        return ResponseEntity.ok(parametresService.getProfil(userId));
    }

    @PutMapping("/{userId}/profile")
    public ResponseEntity<ProfilDto> updateProfil(@PathVariable Long userId, @Valid @RequestBody ProfilDto dto) {
        return ResponseEntity.ok(parametresService.updateProfil(userId, dto));
    }
}
