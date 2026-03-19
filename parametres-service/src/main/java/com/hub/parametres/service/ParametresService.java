package com.hub.parametres.service;

import com.hub.parametres.dto.*;
import com.hub.parametres.entity.*;
import com.hub.parametres.repository.*;
import com.hub.parametres.security.ParametresUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ParametresService {

    private final ProfilParametresRepository profilRepo;
    private final PreferencesParametresRepository preferencesRepo;
    private final SecuriteParametresRepository securiteRepo;
    private final NotificationsParametresRepository notificationsRepo;

    public ParametresUserPrincipal getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof ParametresUserPrincipal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Non authentifié");
        }
        return (ParametresUserPrincipal) auth.getPrincipal();
    }

    private boolean hasRole(String role) {
        return getCurrentUser().getAuthorities().stream()
                .anyMatch(a -> ("ROLE_" + role).equals(a.getAuthority()));
    }

    private boolean canEditOrg() {
        return hasRole("RH") || hasRole("ADMIN");
    }

    public ProfilDto getProfil(Long userId) {
        if (!getCurrentUser().getUserId().equals(userId) && !canEditOrg()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        return profilRepo.findByUserId(userId)
                .map(this::toProfilDto)
                .orElse(ProfilDto.builder().build());
    }

    @Transactional
    public ProfilDto updateProfil(Long userId, ProfilDto dto) {
        if (!getCurrentUser().getUserId().equals(userId) && !canEditOrg()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        ProfilParametres p = profilRepo.findByUserId(userId).orElseGet(() -> {
            ProfilParametres n = new ProfilParametres();
            n.setUserId(userId);
            return n;
        });
        if (getCurrentUser().getUserId().equals(userId)) {
            if (dto.getTelephone() != null) p.setTelephone(dto.getTelephone());
            if (dto.getPhotoUrl() != null) p.setPhotoUrl(dto.getPhotoUrl());
        }
        if (canEditOrg()) {
            if (dto.getEmailPro() != null) p.setEmailPro(dto.getEmailPro());
            if (dto.getPoste() != null) p.setPoste(dto.getPoste());
            if (dto.getDepartement() != null) p.setDepartement(dto.getDepartement());
            if (dto.getLocalisation() != null) p.setLocalisation(dto.getLocalisation());
            if (dto.getTypeContrat() != null) p.setTypeContrat(dto.getTypeContrat());
        }
        p = profilRepo.save(p);
        return toProfilDto(p);
    }

    public PreferencesDto getPreferences(Long userId) {
        if (!getCurrentUser().getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        return preferencesRepo.findByUserId(userId)
                .map(this::toPreferencesDto)
                .orElse(PreferencesDto.builder().language("fr").theme("light").build());
    }

    @Transactional
    public PreferencesDto updatePreferences(Long userId, PreferencesDto dto) {
        if (!getCurrentUser().getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        PreferencesParametres p = preferencesRepo.findByUserId(userId).orElseGet(() -> {
            PreferencesParametres n = new PreferencesParametres();
            n.setUserId(userId);
            return n;
        });
        if (dto.getLanguage() != null) p.setLanguage(dto.getLanguage());
        if (dto.getTheme() != null) p.setTheme(dto.getTheme());
        p = preferencesRepo.save(p);
        return toPreferencesDto(p);
    }

    public SecuriteDto getSecurite(Long userId) {
        if (!getCurrentUser().getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        return securiteRepo.findByUserId(userId)
                .map(this::toSecuriteDto)
                .orElse(SecuriteDto.builder().mfaEnabled(false).build());
    }

    @Transactional
    public SecuriteDto updateSecurite(Long userId, SecuriteDto dto) {
        if (!getCurrentUser().getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        SecuriteParametres s = securiteRepo.findByUserId(userId).orElseGet(() -> {
            SecuriteParametres n = new SecuriteParametres();
            n.setUserId(userId);
            return n;
        });
        if (dto.getMfaEnabled() != null) s.setMfaEnabled(dto.getMfaEnabled());
        if (dto.getPasswordChangedAt() != null) s.setPasswordChangedAt(dto.getPasswordChangedAt());
        s = securiteRepo.save(s);
        return toSecuriteDto(s);
    }

    public NotificationsDto getNotifications(Long userId) {
        if (!getCurrentUser().getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        return notificationsRepo.findByUserId(userId)
                .map(this::toNotificationsDto)
                .orElse(NotificationsDto.builder().emailAlerts(true).pushEnabled(false).smsEnabled(false).build());
    }

    @Transactional
    public NotificationsDto updateNotifications(Long userId, NotificationsDto dto) {
        if (!getCurrentUser().getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        NotificationsParametres n = notificationsRepo.findByUserId(userId).orElseGet(() -> {
            NotificationsParametres e = new NotificationsParametres();
            e.setUserId(userId);
            return e;
        });
        if (dto.getEmailAlerts() != null) n.setEmailAlerts(dto.getEmailAlerts());
        if (dto.getPushEnabled() != null) n.setPushEnabled(dto.getPushEnabled());
        if (dto.getSmsEnabled() != null) n.setSmsEnabled(dto.getSmsEnabled());
        n = notificationsRepo.save(n);
        return toNotificationsDto(n);
    }

    private ProfilDto toProfilDto(ProfilParametres p) {
        return ProfilDto.builder()
                .emailPro(p.getEmailPro())
                .telephone(p.getTelephone())
                .photoUrl(p.getPhotoUrl())
                .poste(p.getPoste())
                .departement(p.getDepartement())
                .localisation(p.getLocalisation())
                .typeContrat(p.getTypeContrat())
                .build();
    }

    private PreferencesDto toPreferencesDto(PreferencesParametres p) {
        return PreferencesDto.builder()
                .language(p.getLanguage())
                .theme(p.getTheme())
                .build();
    }

    private SecuriteDto toSecuriteDto(SecuriteParametres s) {
        return SecuriteDto.builder()
                .mfaEnabled(s.getMfaEnabled())
                .passwordChangedAt(s.getPasswordChangedAt())
                .build();
    }

    private NotificationsDto toNotificationsDto(NotificationsParametres n) {
        return NotificationsDto.builder()
                .emailAlerts(n.getEmailAlerts())
                .pushEnabled(n.getPushEnabled())
                .smsEnabled(n.getSmsEnabled())
                .build();
    }
}
