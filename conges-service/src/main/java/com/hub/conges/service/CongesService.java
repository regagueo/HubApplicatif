package com.hub.conges.service;

import com.hub.conges.dto.*;
import com.hub.conges.entity.DemandeConges;
import com.hub.conges.entity.HistoriqueSolde;
import com.hub.conges.entity.SoldeConges;
import com.hub.conges.entity.ValidationConges;
import com.hub.conges.repository.DemandeCongesRepository;
import com.hub.conges.repository.HistoriqueSoldeRepository;
import com.hub.conges.repository.SoldeCongesRepository;
import com.hub.conges.repository.ValidationCongesRepository;
import com.hub.conges.security.CongesUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CongesService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int ANNEE_COURANTE = LocalDate.now().getYear();

    private final SoldeCongesRepository soldeRepository;
    private final DemandeCongesRepository demandeRepository;
    private final ValidationCongesRepository validationRepository;
    private final HistoriqueSoldeRepository historiqueSoldeRepository;
    private final CalculJoursOuvresService calculJoursOuvresService;

    public CongesUserPrincipal getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CongesUserPrincipal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Non authentifié");
        }
        return (CongesUserPrincipal) auth.getPrincipal();
    }

    private boolean hasRole(String role) {
        return getCurrentUser().getAuthorities().stream()
                .anyMatch(a -> ("ROLE_" + role).equals(a.getAuthority()));
    }

    public List<SoldeCongesDto> getSoldes(Long employeeId) {
        CongesUserPrincipal user = getCurrentUser();
        if (!user.getUserId().equals(employeeId) && !hasRole("MANAGER") && !hasRole("RH") && !hasRole("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        List<SoldeConges> soldes = soldeRepository.findByEmployeeIdAndAnnee(employeeId, ANNEE_COURANTE);
        Map<SoldeConges.TypeSolde, SoldeConges> byType = soldes.stream().collect(Collectors.toMap(SoldeConges::getType, s -> s));
        List<SoldeCongesDto> result = new ArrayList<>();
        for (SoldeConges.TypeSolde t : SoldeConges.TypeSolde.values()) {
            SoldeConges s = byType.get(t);
            double total = s != null ? s.getJoursTotal() : 0;
            double pris = s != null ? s.getJoursPris() : 0;
            result.add(SoldeCongesDto.builder()
                    .type(t.name())
                    .label(typeToLabel(t))
                    .jours(total)
                    .joursPris(pris)
                    .joursRestants(Math.max(0, total - pris))
                    .build());
        }
        return result;
    }

    private static String typeToLabel(SoldeConges.TypeSolde t) {
        switch (t) {
            case ANNUEL: return "Congés Annuels";
            case RTT: return "RTT";
            case EXCEPTIONNEL: return "Congés Exceptionnels";
            case MALADIE: return "Maladie (Total An)";
            default: return t.name();
        }
    }

    @Transactional
    public DemandeCongesDto createDemande(Long employeeId, CreateDemandeRequest request) {
        CongesUserPrincipal current = getCurrentUser();
        Long currentUserId = current.getUserId();
        if (currentUserId == null || !currentUserId.equals(employeeId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        DemandeConges.MotifAbsence motif = DemandeConges.MotifAbsence.valueOf(request.getMotif());
        DemandeConges.PeriodeType periode = DemandeConges.PeriodeType.valueOf(request.getPeriode());
        double dureeJoursOuvres = calculJoursOuvresService.calculerJoursOuvres(request.getDateDebut(), request.getDateFin(), periode);
        DemandeConges entity = DemandeConges.builder()
                .employeeId(employeeId)
                .dateDebut(request.getDateDebut())
                .dateFin(request.getDateFin())
                .motif(motif)
                .periode(periode)
                .commentaire(request.getCommentaire())
                .dureeJoursOuvres(dureeJoursOuvres)
                .statut(DemandeConges.StatutDemande.EN_ATTENTE_MANAGER)
                .dateSoumission(LocalDateTime.now())
                .build();
        entity = demandeRepository.save(entity);
        createValidations(entity);
        return toDto(entity);
    }

    private void createValidations(DemandeConges demande) {
        ValidationConges s = ValidationConges.builder()
                .demande(demande)
                .etape(ValidationConges.EtapeValidation.SOUMISSION)
                .statut(ValidationConges.StatutValidation.VALIDE)
                .dateValidation(LocalDateTime.now())
                .build();
        validationRepository.save(s);
        ValidationConges m = ValidationConges.builder()
                .demande(demande)
                .etape(ValidationConges.EtapeValidation.VALIDATION_MANAGER)
                .statut(ValidationConges.StatutValidation.EN_ATTENTE)
                .build();
        validationRepository.save(m);
        ValidationConges r = ValidationConges.builder()
                .demande(demande)
                .etape(ValidationConges.EtapeValidation.VALIDATION_RH)
                .statut(ValidationConges.StatutValidation.EN_ATTENTE)
                .build();
        validationRepository.save(r);
    }

    public List<DemandeCongesDto> getDemandes(Long employeeId, Integer annee, String statut) {
        CongesUserPrincipal user = getCurrentUser();
        if (!user.getUserId().equals(employeeId) && !hasRole("MANAGER") && !hasRole("RH") && !hasRole("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        List<DemandeConges> list = demandeRepository.findByEmployeeIdOrderByDateSoumissionDesc(employeeId);
        int year = annee != null ? annee : ANNEE_COURANTE;
        list = list.stream()
                .filter(d -> d.getDateDebut().getYear() == year || d.getDateFin().getYear() == year)
                .collect(Collectors.toList());
        if (statut != null && !statut.isBlank() && !"TOUS".equalsIgnoreCase(statut)) {
            try {
                DemandeConges.StatutDemande s = DemandeConges.StatutDemande.valueOf(statut);
                list = list.stream().filter(d -> d.getStatut() == s).collect(Collectors.toList());
            } catch (IllegalArgumentException ignored) {}
        }
        return list.stream().map(this::toDto).collect(Collectors.toList());
    }

    public DemandeCongesDto getDemande(Long demandeId) {
        DemandeConges d = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande introuvable"));
        CongesUserPrincipal user = getCurrentUser();
        if (!d.getEmployeeId().equals(user.getUserId()) && !hasRole("MANAGER") && !hasRole("RH") && !hasRole("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        return toDto(d);
    }

    public List<DemandeCongesDto.SuiviEtapeDto> getSuivi(Long demandeId) {
        DemandeConges d = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande introuvable"));
        CongesUserPrincipal user = getCurrentUser();
        if (!d.getEmployeeId().equals(user.getUserId()) && !hasRole("MANAGER") && !hasRole("RH") && !hasRole("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        List<ValidationConges> validations = validationRepository.findByDemandeIdOrderByEtape(demandeId);
        return validations.stream().map(v -> {
            String label = v.getEtape() == ValidationConges.EtapeValidation.SOUMISSION ? "Soumission" :
                    v.getEtape() == ValidationConges.EtapeValidation.VALIDATION_MANAGER ? "Validation Manager" : "Validation RH";
            String date = v.getDateValidation() != null ? v.getDateValidation().format(DATETIME_FORMAT) : (v.getStatut() == ValidationConges.StatutValidation.EN_ATTENTE ? "En attente" : "");
            if (v.getEtape() == ValidationConges.EtapeValidation.SOUMISSION && v.getDateValidation() != null) {
                date = "Aujourd'hui";
            }
            return new DemandeCongesDto.SuiviEtapeDto(v.getEtape().name(), label, v.getStatut().name(), date);
        }).collect(Collectors.toList());
    }

    @Transactional
    public DemandeCongesDto updateDemande(Long employeeId, Long demandeId, CreateDemandeRequest request) {
        if (!getCurrentUser().getUserId().equals(employeeId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        DemandeConges d = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande introuvable"));
        if (!d.getEmployeeId().equals(employeeId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        if (d.getStatut() != DemandeConges.StatutDemande.SOUMMIS && d.getStatut() != DemandeConges.StatutDemande.EN_ATTENTE_MANAGER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Demande non modifiable");
        }
        d.setDateDebut(request.getDateDebut());
        d.setDateFin(request.getDateFin());
        d.setMotif(DemandeConges.MotifAbsence.valueOf(request.getMotif()));
        d.setPeriode(DemandeConges.PeriodeType.valueOf(request.getPeriode()));
        d.setCommentaire(request.getCommentaire());
        d.setDureeJoursOuvres(calculJoursOuvresService.calculerJoursOuvres(request.getDateDebut(), request.getDateFin(), DemandeConges.PeriodeType.valueOf(request.getPeriode())));
        demandeRepository.save(d);
        return toDto(d);
    }

    @Transactional
    public DemandeCongesDto validerParManager(Long demandeId, String validateurNom) {
        if (!hasRole("MANAGER") && !hasRole("RH") && !hasRole("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Rôle Manager/RH/Admin requis");
        }
        DemandeConges d = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande introuvable"));
        if (d.getStatut() != DemandeConges.StatutDemande.EN_ATTENTE_MANAGER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Demande non en attente Manager");
        }
        List<ValidationConges> validations = validationRepository.findByDemandeIdOrderByEtape(demandeId);
        for (ValidationConges v : validations) {
            if (v.getEtape() == ValidationConges.EtapeValidation.VALIDATION_MANAGER) {
                v.setStatut(ValidationConges.StatutValidation.VALIDE);
                v.setDateValidation(LocalDateTime.now());
                v.setValidateurNom(validateurNom);
                validationRepository.save(v);
                break;
            }
        }
        d.setStatut(DemandeConges.StatutDemande.EN_ATTENTE_RH);
        demandeRepository.save(d);
        return toDto(d);
    }

    @Transactional
    public DemandeCongesDto refuserParManager(Long demandeId, String validateurNom) {
        if (!hasRole("MANAGER") && !hasRole("RH") && !hasRole("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Rôle Manager/RH/Admin requis");
        }
        return refuserDemande(demandeId, ValidationConges.EtapeValidation.VALIDATION_MANAGER, validateurNom);
    }

    @Transactional
    public DemandeCongesDto validerParRH(Long demandeId, String validateurNom) {
        if (!hasRole("RH") && !hasRole("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Rôle RH/Admin requis");
        }
        DemandeConges d = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande introuvable"));
        if (d.getStatut() != DemandeConges.StatutDemande.EN_ATTENTE_RH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Demande non en attente RH");
        }
        List<ValidationConges> validations = validationRepository.findByDemandeIdOrderByEtape(demandeId);
        for (ValidationConges v : validations) {
            if (v.getEtape() == ValidationConges.EtapeValidation.VALIDATION_RH) {
                v.setStatut(ValidationConges.StatutValidation.VALIDE);
                v.setDateValidation(LocalDateTime.now());
                v.setValidateurNom(validateurNom);
                validationRepository.save(v);
                break;
            }
        }
        d.setStatut(DemandeConges.StatutDemande.VALIDE);
        d.setValidateurNom(validateurNom);
        demandeRepository.save(d);
        decrementSolde(d);
        return toDto(d);
    }

    @Transactional
    public DemandeCongesDto refuserParRH(Long demandeId, String validateurNom) {
        if (!hasRole("RH") && !hasRole("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Rôle RH/Admin requis");
        }
        return refuserDemande(demandeId, ValidationConges.EtapeValidation.VALIDATION_RH, validateurNom);
    }

    private DemandeCongesDto refuserDemande(Long demandeId, ValidationConges.EtapeValidation etape, String validateurNom) {
        DemandeConges d = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande introuvable"));
        List<ValidationConges> validations = validationRepository.findByDemandeIdOrderByEtape(demandeId);
        for (ValidationConges v : validations) {
            if (v.getEtape() == etape) {
                v.setStatut(ValidationConges.StatutValidation.REFUSE);
                v.setDateValidation(LocalDateTime.now());
                v.setValidateurNom(validateurNom);
                validationRepository.save(v);
                break;
            }
        }
        d.setStatut(DemandeConges.StatutDemande.REFUSE);
        d.setValidateurNom(validateurNom);
        demandeRepository.save(d);
        return toDto(d);
    }

    private void decrementSolde(DemandeConges d) {
        double jours = d.getDureeJoursOuvres() != null ? d.getDureeJoursOuvres() : 0;
        if (jours <= 0) return;
        int annee = d.getDateDebut().getYear();
        Optional<SoldeConges> opt = soldeRepository.findByEmployeeIdAndAnneeAndType(d.getEmployeeId(), annee, SoldeConges.TypeSolde.ANNUEL);
        if (opt.isEmpty()) return;
        SoldeConges s = opt.get();
        s.setJoursPris(s.getJoursPris() + jours);
        s.setDateDerniereMiseAJour(LocalDateTime.now());
        soldeRepository.save(s);
        historiqueSoldeRepository.save(HistoriqueSolde.builder()
                .employeId(d.getEmployeeId())
                .typeMouvement(HistoriqueSolde.TypeMouvement.CONSOMMATION)
                .valeur(-jours)
                .dateMouvement(LocalDateTime.now())
                .referenceDemandeId(d.getId())
                .annee(annee)
                .libelle("Congés consommés - demande #" + d.getId())
                .build());
    }

    public List<DemandeCongesDto> getDemandesEnAttente(String etape) {
        if (!hasRole("MANAGER") && !hasRole("RH") && !hasRole("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        List<DemandeConges.StatutDemande> statuts = "MANAGER".equalsIgnoreCase(etape)
                ? List.of(DemandeConges.StatutDemande.EN_ATTENTE_MANAGER)
                : List.of(DemandeConges.StatutDemande.EN_ATTENTE_RH);
        return demandeRepository.findByStatutInOrderByDateSoumissionDesc(statuts).stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<DemandeCongesDto> getDemandesValidation(String etape) {
        if (!hasRole("MANAGER") && !hasRole("RH") && !hasRole("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        List<DemandeConges.StatutDemande> statuts = List.of(
                DemandeConges.StatutDemande.EN_ATTENTE_MANAGER,
                DemandeConges.StatutDemande.EN_ATTENTE_RH,
                DemandeConges.StatutDemande.VALIDE,
                DemandeConges.StatutDemande.REFUSE
        );
        List<DemandeCongesDto> all = demandeRepository.findByStatutInOrderByDateSoumissionDesc(statuts)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        if ("RH".equalsIgnoreCase(etape)) {
            return all.stream()
                    .filter(d -> !"EN_ATTENTE_MANAGER".equals(d.getStatut()))
                    .collect(Collectors.toList());
        }
        return all;
    }

    public List<HistoriqueSoldeDto> getHistoriqueSolde(Long employeeId, Integer anneeParam) {
        CongesUserPrincipal user = getCurrentUser();
        if (!user.getUserId().equals(employeeId) && !hasRole("MANAGER") && !hasRole("RH") && !hasRole("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        int annee = anneeParam != null ? anneeParam : ANNEE_COURANTE;
        return historiqueSoldeRepository.findByEmployeIdAndAnneeOrderByDateMouvementDesc(employeeId, annee).stream()
                .map(h -> new HistoriqueSoldeDto(h.getId(), h.getTypeMouvement().name(), h.getValeur(), h.getDateMouvement(), h.getReferenceDemandeId(), h.getLibelle()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SoldeEmployeDto> getAllSoldesForRh(Integer anneeParam) {
        if (!hasRole("RH") && !hasRole("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Rôle RH/Admin requis");
        }
        int annee = anneeParam != null ? anneeParam : ANNEE_COURANTE;
        List<Long> employeeIds = soldeRepository.findDistinctEmployeeIdsByAnnee(annee);
        List<SoldeEmployeDto> result = new ArrayList<>();
        for (Long empId : employeeIds) {
            List<SoldeConges> soldes = soldeRepository.findByEmployeeIdAndAnnee(empId, annee);
            for (SoldeConges s : soldes) {
                result.add(SoldeEmployeDto.builder()
                        .employeeId(empId)
                        .type(s.getType().name())
                        .label(typeToLabel(s.getType()))
                        .joursTotal(s.getJoursTotal())
                        .joursPris(s.getJoursPris())
                        .joursRestants(s.getJoursRestants())
                        .annee(annee)
                        .build());
            }
        }
        return result;
    }

    public CalculJoursOuvresDto calculJoursOuvres(LocalDate dateDebut, LocalDate dateFin, String periode) {
        CalculJoursOuvresService.ResultatCalcul r = calculJoursOuvresService.calculer(dateDebut, dateFin, "DEMI_JOURNEE".equals(periode));
        return new CalculJoursOuvresDto(r.getJoursOuvres(), r.getWeekEndsExclus(), r.getJoursFeriesExclus());
    }

    @Transactional
    public void annulerDemande(Long employeeId, Long demandeId) {
        if (!getCurrentUser().getUserId().equals(employeeId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        DemandeConges d = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande introuvable"));
        if (!d.getEmployeeId().equals(employeeId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        if (d.getStatut() == DemandeConges.StatutDemande.VALIDE || d.getStatut() == DemandeConges.StatutDemande.REFUSE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Demande non annulable");
        }
        demandeRepository.delete(d);
    }

    private DemandeCongesDto toDto(DemandeConges d) {
        double duree = d.getDureeJoursOuvres() != null ? d.getDureeJoursOuvres()
                : (d.getPeriode() == DemandeConges.PeriodeType.DEMI_JOURNEE && d.getDateDebut().equals(d.getDateFin()) ? 0.5
                : ChronoUnit.DAYS.between(d.getDateDebut(), d.getDateFin()) + 1);
        List<ValidationConges> validations = validationRepository.findByDemandeIdOrderByEtape(d.getId());
        List<DemandeCongesDto.SuiviEtapeDto> suivi = validations.stream().map(v -> {
            String label = v.getEtape() == ValidationConges.EtapeValidation.SOUMISSION ? "Soumission" :
                    v.getEtape() == ValidationConges.EtapeValidation.VALIDATION_MANAGER ? "Validation Manager" : "Validation RH";
            String date = v.getDateValidation() != null ? (v.getEtape() == ValidationConges.EtapeValidation.SOUMISSION ? "Aujourd'hui" : v.getDateValidation().format(DATETIME_FORMAT)) : "En attente";
            return new DemandeCongesDto.SuiviEtapeDto(v.getEtape().name(), label, v.getStatut().name(), date);
        }).collect(Collectors.toList());
        return DemandeCongesDto.builder()
                .id(d.getId())
                .employeeId(d.getEmployeeId())
                .dateDebut(d.getDateDebut().format(DATE_FORMAT))
                .dateFin(d.getDateFin().format(DATE_FORMAT))
                .motif(motifToLabel(d.getMotif()))
                .motifCode(d.getMotif().name())
                .periode(d.getPeriode().name())
                .commentaire(d.getCommentaire())
                .statut(d.getStatut().name())
                .statutLabel(statutToLabel(d.getStatut()))
                .dateSoumission(d.getDateSoumission().format(DATE_FORMAT))
                .dureeJours(duree)
                .validateurNom(d.getValidateurNom() != null ? d.getValidateurNom() : "--")
                .suivi(suivi)
                .build();
    }

    private static String motifToLabel(DemandeConges.MotifAbsence m) {
        switch (m) {
            case CONGES_ANNUELS: return "Congés Annuels";
            case RTT: return "RTT";
            case EVENEMENT_FAMILIAL: return "Événement familial";
            case MALADIE: return "Maladie";
            case CONGES_EXCEPTIONNELS: return "Congés Exceptionnels";
            default: return m.name();
        }
    }

    private static String statutToLabel(DemandeConges.StatutDemande s) {
        switch (s) {
            case SOUMMIS: return "Soumis";
            case EN_ATTENTE_MANAGER: return "En attente Manager";
            case EN_ATTENTE_RH: return "En attente RH";
            case VALIDE: return "Validé";
            case REFUSE: return "Refusé";
            default: return s.name();
        }
    }
}
