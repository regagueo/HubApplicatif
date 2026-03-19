package com.hub.employee.service;

import com.hub.employee.dto.*;
import com.hub.employee.entity.*;
import com.hub.employee.repository.*;
import com.hub.employee.security.EmployeeUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private static final java.time.format.DateTimeFormatter FRENCH_DATE =
            java.time.format.DateTimeFormatter.ofPattern("d MMM", Locale.FRENCH);

    private final DemandeCongesRepository congesRepository;
    private final NoteFraisRepository noteFraisRepository;
    private final AlerteRepository alerteRepository;
    private final CollaborateurRepository collaborateurRepository;

    public EmployeeUserPrincipal getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof EmployeeUserPrincipal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Non authentifié");
        }
        return (EmployeeUserPrincipal) auth.getPrincipal();
    }

    void ensureAccessToEmployee(Long employeeId) {
        EmployeeUserPrincipal user = getCurrentUser();
        if (!user.getUserId().equals(employeeId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé à cette ressource");
        }
    }

    // --- Indicators ---
    @Transactional(readOnly = true)
    public IndicatorsDto getIndicators(Long employeeId) {
        ensureAccessToEmployee(employeeId);

        List<DemandeConges> congesValides = congesRepository.findByEmployeeIdOrderByDateDebutDesc(employeeId)
                .stream().filter(d -> d.getStatut() == DemandeConges.StatutConges.VALIDE).toList();
        int joursPris = congesValides.stream()
                .mapToInt(d -> (int) ChronoUnit.DAYS.between(d.getDateDebut(), d.getDateFin()) + 1)
                .sum();
        int totalJours = 22;
        int joursRestants = Math.max(0, totalJours - joursPris);

        double enAttente = noteFraisRepository.findByEmployeeIdAndStatut(employeeId, NoteFrais.StatutFrais.EN_ATTENTE)
                .stream().mapToDouble(n -> n.getMontant().doubleValue()).sum();

        return IndicatorsDto.builder()
                .joursPris(joursPris)
                .joursRestants(joursRestants)
                .totalJours(totalJours)
                .notesFraisEnAttente(enAttente)
                .delaiMoyenJours(3)
                .build();
    }

    // --- Alerts ---
    @Transactional(readOnly = true)
    public List<AlerteDto> getAlerts(Long employeeId) {
        ensureAccessToEmployee(employeeId);
        return alerteRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId)
                .stream().map(this::toAlerteDto).collect(Collectors.toList());
    }

    private String formatRelativeDate(Instant instant) {
        long hours = ChronoUnit.HOURS.between(instant, Instant.now());
        if (hours < 1) return "À L'INSTANT";
        if (hours < 24) return "IL Y A " + hours + "H";
        long days = ChronoUnit.DAYS.between(instant, Instant.now());
        if (days == 1) return "HIER";
        if (days < 7) return days + " JOURS";
        return FRENCH_DATE.format(instant.atZone(ZoneId.systemDefault()));
    }

    private AlerteDto toAlerteDto(Alerte a) {
        return AlerteDto.builder()
                .id(a.getId())
                .type(a.getType())
                .titre(a.getTitre())
                .message(a.getMessage())
                .date(formatRelativeDate(a.getCreatedAt()))
                .build();
    }

    // --- Congés (CRUD) ---
    @Transactional(readOnly = true)
    public List<DemandeCongesDto> getConges(Long employeeId) {
        ensureAccessToEmployee(employeeId);
        return congesRepository.findByEmployeeIdOrderByDateDebutDesc(employeeId)
                .stream().map(this::toDemandeCongesDto).collect(Collectors.toList());
    }

    private DemandeCongesDto toDemandeCongesDto(DemandeConges d) {
        return DemandeCongesDto.builder()
                .id(d.getId())
                .type(d.getType())
                .dateDebut(d.getDateDebut().format(FRENCH_DATE))
                .dateFin(d.getDateFin().format(FRENCH_DATE))
                .statut(d.getStatut().name())
                .build();
    }

    @Transactional
    public DemandeCongesDto createDemandeConges(Long employeeId, CreateDemandeCongesRequest request) {
        ensureAccessToEmployee(employeeId);
        DemandeConges entity = DemandeConges.builder()
                .employeeId(employeeId)
                .type(request.getType())
                .dateDebut(request.getDateDebut())
                .dateFin(request.getDateFin())
                .statut(DemandeConges.StatutConges.EN_ATTENTE)
                .build();
        entity = congesRepository.save(entity);
        return toDemandeCongesDto(entity);
    }

    @Transactional
    public DemandeCongesDto updateDemandeConges(Long employeeId, Long congesId, CreateDemandeCongesRequest request) {
        ensureAccessToEmployee(employeeId);
        DemandeConges entity = congesRepository.findById(congesId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande introuvable"));
        if (!entity.getEmployeeId().equals(employeeId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        entity.setType(request.getType());
        entity.setDateDebut(request.getDateDebut());
        entity.setDateFin(request.getDateFin());
        entity = congesRepository.save(entity);
        return toDemandeCongesDto(entity);
    }

    @Transactional
    public void deleteDemandeConges(Long employeeId, Long congesId) {
        ensureAccessToEmployee(employeeId);
        DemandeConges entity = congesRepository.findById(congesId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande introuvable"));
        if (!entity.getEmployeeId().equals(employeeId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        congesRepository.delete(entity);
    }

    // --- Notes de frais (CRUD) ---
    @Transactional(readOnly = true)
    public NotesFraisResponseDto getNotesFrais(Long employeeId) {
        ensureAccessToEmployee(employeeId);
        List<NoteFrais> all = noteFraisRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId);
        double enAttente = all.stream()
                .filter(n -> n.getStatut() == NoteFrais.StatutFrais.EN_ATTENTE)
                .mapToDouble(n -> n.getMontant().doubleValue())
                .sum();
        double valide = all.stream()
                .filter(n -> n.getStatut() == NoteFrais.StatutFrais.VALIDE)
                .mapToDouble(n -> n.getMontant().doubleValue())
                .sum();
        List<NotesFraisResponseDto.RemboursementDto> derniers = noteFraisRepository
                .findByEmployeeIdOrderByCreatedAtDesc(employeeId)
                .stream()
                .filter(n -> n.getStatut() == NoteFrais.StatutFrais.VALIDE)
                .limit(5)
                .map(n -> new NotesFraisResponseDto.RemboursementDto(n.getId(), n.getLibelle(), n.getMontant().doubleValue()))
                .collect(Collectors.toList());

        return NotesFraisResponseDto.builder()
                .enAttente(enAttente)
                .valide(valide)
                .derniersRemboursements(derniers)
                .build();
    }

    @Transactional
    public NotesFraisResponseDto.RemboursementDto createNoteFrais(Long employeeId, CreateNoteFraisRequest request) {
        ensureAccessToEmployee(employeeId);
        NoteFrais entity = NoteFrais.builder()
                .employeeId(employeeId)
                .libelle(request.getLibelle())
                .montant(request.getMontant())
                .statut(NoteFrais.StatutFrais.EN_ATTENTE)
                .createdAt(Instant.now())
                .build();
        entity = noteFraisRepository.save(entity);
        return new NotesFraisResponseDto.RemboursementDto(entity.getId(), entity.getLibelle(), entity.getMontant().doubleValue());
    }

    @Transactional
    public void updateNoteFrais(Long employeeId, Long fraisId, CreateNoteFraisRequest request) {
        ensureAccessToEmployee(employeeId);
        NoteFrais entity = noteFraisRepository.findById(fraisId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Note de frais introuvable"));
        if (!entity.getEmployeeId().equals(employeeId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        entity.setLibelle(request.getLibelle());
        entity.setMontant(request.getMontant());
        noteFraisRepository.save(entity);
    }

    @Transactional
    public void deleteNoteFrais(Long employeeId, Long fraisId) {
        ensureAccessToEmployee(employeeId);
        NoteFrais entity = noteFraisRepository.findById(fraisId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Note de frais introuvable"));
        if (!entity.getEmployeeId().equals(employeeId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        noteFraisRepository.delete(entity);
    }

    // --- Collaborateurs ---
    @Transactional(readOnly = true)
    public List<CollaborateurDto> getCollaborateurs(Long employeeId) {
        ensureAccessToEmployee(employeeId);
        return collaborateurRepository.findAllByOrderByNom()
                .stream()
                .map(c -> CollaborateurDto.builder()
                        .id(c.getId())
                        .nom(c.getNom())
                        .service(c.getService())
                        .build())
                .collect(Collectors.toList());
    }
}
