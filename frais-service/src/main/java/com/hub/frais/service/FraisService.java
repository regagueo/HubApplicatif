package com.hub.frais.service;

import com.hub.frais.dto.DemandeFraisDto;
import com.hub.frais.dto.DossierFraisDto;
import com.hub.frais.dto.EncoursFraisDto;
import com.hub.frais.entity.DemandeFrais;
import com.hub.frais.entity.DossierFrais;
import com.hub.frais.entity.PlafondFrais;
import com.hub.frais.repository.DemandeFraisRepository;
import com.hub.frais.repository.DossierFraisRepository;
import com.hub.frais.security.FraisUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FraisService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final List<DemandeFrais.StatutDemande> ENCOURS_STATUTS = List.of(
            DemandeFrais.StatutDemande.EN_ATTENTE_MANAGER,
            DemandeFrais.StatutDemande.EN_ATTENTE_COMPTABILITE,
            DemandeFrais.StatutDemande.VALIDE
    );

    private final DemandeFraisRepository demandeRepository;
    private final DossierFraisRepository dossierRepository;
    private final JustificatifStorageService justificatifStorage;
    private final PlafondFraisService plafondFraisService;

    public FraisUserPrincipal getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof FraisUserPrincipal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Non authentifié");
        }
        return (FraisUserPrincipal) auth.getPrincipal();
    }

    private boolean hasRole(String role) {
        return getCurrentUser().getAuthorities().stream()
                .anyMatch(a -> ("ROLE_" + role).equals(a.getAuthority()));
    }

    private void checkEmployeeAccess(Long employeeId) {
        FraisUserPrincipal user = getCurrentUser();
        if (!user.getUserId().equals(employeeId) && !hasRole("MANAGER") && !hasRole("COMPTABILITE") && !hasRole("RH") && !hasRole("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
    }

    @Transactional
    public DemandeFraisDto createDemande(Long employeeId,
                                         BigDecimal montant,
                                         String categorie,
                                         String modeTransport,
                                         BigDecimal kilometres,
                                         String ville,
                                         Integer anneesExperience,
                                         String description,
                                         MultipartFile justificatif) {
        return createDemandeInterne(
                employeeId,
                null,
                montant,
                categorie,
                modeTransport,
                kilometres,
                ville,
                anneesExperience,
                description,
                justificatif
        );
    }

    @Transactional
    public DemandeFraisDto createDemandeDansDossier(Long employeeId,
                                                    Long dossierId,
                                                    BigDecimal montant,
                                                    String categorie,
                                                    String modeTransport,
                                                    BigDecimal kilometres,
                                                    String ville,
                                                    Integer anneesExperience,
                                                    String description,
                                                    MultipartFile justificatif) {
        DossierFrais dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dossier introuvable"));
        if (!dossier.getEmployeeId().equals(employeeId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        if (dossier.getStatut() != DossierFrais.StatutDossier.BROUILLON) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le dossier n'est plus modifiable");
        }
        return createDemandeInterne(
                employeeId,
                dossierId,
                montant,
                categorie,
                modeTransport,
                kilometres,
                ville,
                anneesExperience,
                description,
                justificatif
        );
    }

    private DemandeFraisDto createDemandeInterne(Long employeeId,
                                                 Long dossierId,
                                                 BigDecimal montant,
                                                 String categorie,
                                                 String modeTransport,
                                                 BigDecimal kilometres,
                                                 String ville,
                                                 Integer anneesExperience,
                                                 String description,
                                                 MultipartFile justificatif) {
        if (!getCurrentUser().getUserId().equals(employeeId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        DemandeFrais.CategorieFrais cat = DemandeFrais.CategorieFrais.valueOf(categorie);
        DemandeFrais.ModeTransport mode = modeTransport != null && !modeTransport.isBlank()
                ? DemandeFrais.ModeTransport.valueOf(modeTransport)
                : null;
        PlafondFrais plafonds = plafondFraisService.getPlafonds();
        BigDecimal montantFinal = appliquerReglesMetier(cat, mode, montant, kilometres, ville, anneesExperience, justificatif, plafonds);
        DemandeFrais entity = DemandeFrais.builder()
                .employeeId(employeeId)
                .dossierId(dossierId)
                .montant(montantFinal)
                .categorie(cat)
                .modeTransport(mode)
                .kilometres(kilometres)
                .ville(ville)
                .anneesExperience(anneesExperience)
                .description(description != null ? description : "")
                .statut(DemandeFrais.StatutDemande.EN_ATTENTE_MANAGER)
                .dateSoumission(LocalDateTime.now())
                .build();
        entity = demandeRepository.save(entity);
        entity.setReference("EXP-" + java.time.Year.now().getValue() + "-" + String.format("%04d", entity.getId()));
        entity = demandeRepository.save(entity);
        if (justificatif != null && !justificatif.isEmpty()) {
            try {
                entity.setJustificatifFileId(justificatifStorage.store(entity.getId(), justificatif));
                entity = demandeRepository.save(entity);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
            }
        }
        return toDto(entity);
    }

    private BigDecimal appliquerReglesMetier(DemandeFrais.CategorieFrais categorie,
                                             DemandeFrais.ModeTransport modeTransport,
                                             BigDecimal montantSaisi,
                                             BigDecimal kilometres,
                                             String ville,
                                             Integer anneesExperience,
                                             MultipartFile justificatif,
                                             PlafondFrais plafonds) {
        BigDecimal montant = montantSaisi != null ? montantSaisi : BigDecimal.ZERO;
        boolean hasJustificatif = justificatif != null && !justificatif.isEmpty();

        switch (categorie) {
            case TRANSPORT:
                if (modeTransport == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le mode de transport est obligatoire");
                }
                switch (modeTransport) {
                    case VOITURE_PERSONNELLE:
                        if (kilometres == null || kilometres.compareTo(BigDecimal.ZERO) <= 0) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le kilométrage est obligatoire pour voiture personnelle");
                        }
                        return kilometres.multiply(plafonds.getTransportTarifKm()).setScale(2, RoundingMode.HALF_UP);
                    case TRAIN_BUS_AVION:
                        if (!hasJustificatif) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Justificatif obligatoire pour train/bus/avion");
                        }
                        return montant;
                    case TAXI_COVOITURAGE:
                        if (!hasJustificatif) {
                            return plafonds.getTaxiForfaitSansJustificatif().setScale(2, RoundingMode.HALF_UP);
                        }
                        return montant;
                    case PEAGE_CARBURANT:
                        if (!hasJustificatif) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Justificatif obligatoire pour péage/carburant");
                        }
                        return montant;
                }
                break;
            case REPAS:
                int exp = anneesExperience != null ? anneesExperience : 0;
                BigDecimal plafondRepas = exp < 3 ? plafonds.getRepasMoins3Ans() : plafonds.getRepas3AnsEtPlus();
                if (exp >= 3 && plafondRepas.compareTo(plafonds.getRepas3AnsEtPlusMax()) > 0) {
                    plafondRepas = plafonds.getRepas3AnsEtPlusMax();
                }
                if (montant.compareTo(plafondRepas) > 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Montant repas dépasse le plafond autorisé (" + plafondRepas + " DH)");
                }
                return montant;
            case HEBERGEMENT:
                if (!hasJustificatif) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Justificatif obligatoire pour hébergement");
                }
                BigDecimal plafondHebergement = "CASABLANCA".equalsIgnoreCase(ville != null ? ville.trim() : "")
                        ? plafonds.getHebergementCasablanca()
                        : plafonds.getHebergementAutresVilles();
                if (montant.compareTo(plafondHebergement) > 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Montant hébergement dépasse le plafond autorisé (" + plafondHebergement + " DH)");
                }
                return montant;
            case FOURNITURES:
                if (!hasJustificatif) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Justificatif obligatoire pour fournitures");
                }
                return montant;
            case AUTRE:
                if (!hasJustificatif) {
                    if (!Boolean.TRUE.equals(plafonds.getPerDiemActif())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Justificatif obligatoire pour frais divers");
                    }
                    return plafonds.getPerDiemGlobal().setScale(2, RoundingMode.HALF_UP);
                }
                return montant;
            default:
                return montant;
        }
        return montant;
    }

    public EncoursFraisDto getEncours(Long employeeId) {
        checkEmployeeAccess(employeeId);
        List<DemandeFrais> all = demandeRepository.findByEmployeeIdOrderByDateSoumissionDesc(employeeId);
        List<DemandeFrais> encoursList = all.stream()
                .filter(d -> ENCOURS_STATUTS.contains(d.getStatut()))
                .limit(10)
                .collect(Collectors.toList());
        BigDecimal montantEncours = all.stream()
                .filter(d -> ENCOURS_STATUTS.contains(d.getStatut()))
                .map(DemandeFrais::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return EncoursFraisDto.builder()
                .montantEncours(montantEncours)
                .demandesEncours(encoursList.stream().map(this::toDto).collect(Collectors.toList()))
                .build();
    }

    public List<DemandeFraisDto> getHistorique(Long employeeId) {
        checkEmployeeAccess(employeeId);
        return demandeRepository.findByEmployeeIdAndDossierIdIsNullOrderByDateSoumissionDesc(employeeId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public DossierFraisDto createDossier(Long employeeId, String titre, LocalDate dateDebut, LocalDate dateFin) {
        if (!getCurrentUser().getUserId().equals(employeeId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        DossierFrais dossier = dossierRepository.save(DossierFrais.builder()
                .employeeId(employeeId)
                .titre(titre)
                .dateDebut(dateDebut)
                .dateFin(dateFin)
                .statut(DossierFrais.StatutDossier.BROUILLON)
                .dateCreation(LocalDateTime.now())
                .build());
        return toDossierDto(dossier);
    }

    @Transactional
    public DossierFraisDto soumettreDossier(Long employeeId, Long dossierId) {
        DossierFrais dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dossier introuvable"));
        if (!dossier.getEmployeeId().equals(employeeId) || !getCurrentUser().getUserId().equals(employeeId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        List<DemandeFrais> notes = demandeRepository.findByDossierIdOrderByDateSoumissionAsc(dossierId);
        if (notes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ajoutez au moins une note dans le dossier");
        }
        dossier.setStatut(DossierFrais.StatutDossier.EN_ATTENTE_MANAGER);
        dossier.setDateSoumission(LocalDateTime.now());
        dossierRepository.save(dossier);
        for (DemandeFrais n : notes) {
            n.setStatut(DemandeFrais.StatutDemande.EN_ATTENTE_MANAGER);
            demandeRepository.save(n);
        }
        return toDossierDto(dossier);
    }

    public List<DossierFraisDto> getHistoriqueDossiers(Long employeeId) {
        checkEmployeeAccess(employeeId);
        return dossierRepository.findByEmployeeIdOrderByDateCreationDesc(employeeId).stream()
                .map(this::toDossierDto)
                .collect(Collectors.toList());
    }

    public DossierFraisDto getDossier(Long dossierId) {
        DossierFrais dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dossier introuvable"));
        checkEmployeeAccess(dossier.getEmployeeId());
        return toDossierDto(dossier);
    }

    public List<DossierFraisDto> getDossiersValidation(String etape) {
        if (!hasRole("MANAGER") && !hasRole("RH") && !hasRole("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        List<DossierFrais.StatutDossier> statuts = List.of(
                DossierFrais.StatutDossier.EN_ATTENTE_MANAGER,
                DossierFrais.StatutDossier.EN_ATTENTE_RH,
                DossierFrais.StatutDossier.VALIDE,
                DossierFrais.StatutDossier.REFUSE
        );
        List<DossierFraisDto> all = dossierRepository.findByStatutInOrderByDateCreationDesc(statuts)
                .stream().map(this::toDossierDto).collect(Collectors.toList());
        if ("RH".equalsIgnoreCase(etape)) {
            return all.stream().filter(d -> !"EN_ATTENTE_MANAGER".equals(d.getStatut())).collect(Collectors.toList());
        }
        return all;
    }

    public List<DemandeFraisDto> getDemandesValidation(String etape) {
        if (!hasRole("MANAGER") && !hasRole("RH") && !hasRole("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        List<DemandeFrais.StatutDemande> statuts = List.of(
                DemandeFrais.StatutDemande.EN_ATTENTE_MANAGER,
                DemandeFrais.StatutDemande.EN_ATTENTE_COMPTABILITE,
                DemandeFrais.StatutDemande.VALIDE,
                DemandeFrais.StatutDemande.REFUSE
        );
        List<DemandeFraisDto> all = demandeRepository.findByStatutInOrderByDateSoumissionDesc(statuts)
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

    @Transactional
    public DemandeFraisDto validerParManager(Long demandeId, String validateurNom) {
        if (!hasRole("MANAGER") && !hasRole("RH") && !hasRole("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Rôle Manager/RH/Admin requis");
        }
        DemandeFrais d = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande introuvable"));
        if (d.getStatut() != DemandeFrais.StatutDemande.EN_ATTENTE_MANAGER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Demande non en attente Manager");
        }
        d.setManagerNom(validateurNom);
        d.setManagerDecisionDate(LocalDateTime.now());
        d.setStatut(DemandeFrais.StatutDemande.EN_ATTENTE_COMPTABILITE);
        return toDto(demandeRepository.save(d));
    }

    @Transactional
    public DemandeFraisDto refuserParManager(Long demandeId, String validateurNom) {
        if (!hasRole("MANAGER") && !hasRole("RH") && !hasRole("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Rôle Manager/RH/Admin requis");
        }
        DemandeFrais d = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande introuvable"));
        if (d.getStatut() != DemandeFrais.StatutDemande.EN_ATTENTE_MANAGER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Demande non en attente Manager");
        }
        d.setManagerNom(validateurNom);
        d.setManagerDecisionDate(LocalDateTime.now());
        d.setStatut(DemandeFrais.StatutDemande.REFUSE);
        return toDto(demandeRepository.save(d));
    }

    @Transactional
    public DemandeFraisDto validerParRh(Long demandeId, String validateurNom) {
        if (!hasRole("RH") && !hasRole("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Rôle RH/Admin requis");
        }
        DemandeFrais d = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande introuvable"));
        if (d.getStatut() != DemandeFrais.StatutDemande.EN_ATTENTE_COMPTABILITE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Demande non en attente RH");
        }
        d.setRhNom(validateurNom);
        d.setRhDecisionDate(LocalDateTime.now());
        d.setDateRemboursement(LocalDate.now());
        d.setStatut(DemandeFrais.StatutDemande.VALIDE);
        return toDto(demandeRepository.save(d));
    }

    @Transactional
    public DemandeFraisDto refuserParRh(Long demandeId, String validateurNom) {
        if (!hasRole("RH") && !hasRole("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Rôle RH/Admin requis");
        }
        DemandeFrais d = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande introuvable"));
        if (d.getStatut() != DemandeFrais.StatutDemande.EN_ATTENTE_COMPTABILITE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Demande non en attente RH");
        }
        d.setRhNom(validateurNom);
        d.setRhDecisionDate(LocalDateTime.now());
        d.setStatut(DemandeFrais.StatutDemande.REFUSE);
        return toDto(demandeRepository.save(d));
    }

    @Transactional
    public DossierFraisDto validerDossierParManager(Long dossierId, String validateurNom) {
        if (!hasRole("MANAGER") && !hasRole("RH") && !hasRole("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Rôle Manager/RH/Admin requis");
        }
        DossierFrais dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dossier introuvable"));
        if (dossier.getStatut() != DossierFrais.StatutDossier.EN_ATTENTE_MANAGER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dossier non en attente Manager");
        }
        dossier.setManagerNom(validateurNom);
        dossier.setManagerDecisionDate(LocalDateTime.now());
        dossier.setStatut(DossierFrais.StatutDossier.EN_ATTENTE_RH);
        dossierRepository.save(dossier);
        for (DemandeFrais n : demandeRepository.findByDossierIdOrderByDateSoumissionAsc(dossierId)) {
            n.setManagerNom(validateurNom);
            n.setManagerDecisionDate(dossier.getManagerDecisionDate());
            n.setStatut(DemandeFrais.StatutDemande.EN_ATTENTE_COMPTABILITE);
            demandeRepository.save(n);
        }
        return toDossierDto(dossier);
    }

    @Transactional
    public DossierFraisDto refuserDossierParManager(Long dossierId, String validateurNom) {
        if (!hasRole("MANAGER") && !hasRole("RH") && !hasRole("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Rôle Manager/RH/Admin requis");
        }
        DossierFrais dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dossier introuvable"));
        if (dossier.getStatut() != DossierFrais.StatutDossier.EN_ATTENTE_MANAGER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dossier non en attente Manager");
        }
        dossier.setManagerNom(validateurNom);
        dossier.setManagerDecisionDate(LocalDateTime.now());
        dossier.setStatut(DossierFrais.StatutDossier.REFUSE);
        dossierRepository.save(dossier);
        for (DemandeFrais n : demandeRepository.findByDossierIdOrderByDateSoumissionAsc(dossierId)) {
            n.setManagerNom(validateurNom);
            n.setManagerDecisionDate(dossier.getManagerDecisionDate());
            n.setStatut(DemandeFrais.StatutDemande.REFUSE);
            demandeRepository.save(n);
        }
        return toDossierDto(dossier);
    }

    @Transactional
    public DossierFraisDto validerDossierParRh(Long dossierId, String validateurNom) {
        if (!hasRole("RH") && !hasRole("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Rôle RH/Admin requis");
        }
        DossierFrais dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dossier introuvable"));
        if (dossier.getStatut() != DossierFrais.StatutDossier.EN_ATTENTE_RH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dossier non en attente RH");
        }
        dossier.setRhNom(validateurNom);
        dossier.setRhDecisionDate(LocalDateTime.now());
        dossier.setStatut(DossierFrais.StatutDossier.VALIDE);
        dossierRepository.save(dossier);
        for (DemandeFrais n : demandeRepository.findByDossierIdOrderByDateSoumissionAsc(dossierId)) {
            n.setRhNom(validateurNom);
            n.setRhDecisionDate(dossier.getRhDecisionDate());
            n.setDateRemboursement(LocalDate.now());
            n.setStatut(DemandeFrais.StatutDemande.VALIDE);
            demandeRepository.save(n);
        }
        return toDossierDto(dossier);
    }

    @Transactional
    public DossierFraisDto refuserDossierParRh(Long dossierId, String validateurNom) {
        if (!hasRole("RH") && !hasRole("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Rôle RH/Admin requis");
        }
        DossierFrais dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dossier introuvable"));
        if (dossier.getStatut() != DossierFrais.StatutDossier.EN_ATTENTE_RH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dossier non en attente RH");
        }
        dossier.setRhNom(validateurNom);
        dossier.setRhDecisionDate(LocalDateTime.now());
        dossier.setStatut(DossierFrais.StatutDossier.REFUSE);
        dossierRepository.save(dossier);
        for (DemandeFrais n : demandeRepository.findByDossierIdOrderByDateSoumissionAsc(dossierId)) {
            n.setRhNom(validateurNom);
            n.setRhDecisionDate(dossier.getRhDecisionDate());
            n.setStatut(DemandeFrais.StatutDemande.REFUSE);
            demandeRepository.save(n);
        }
        return toDossierDto(dossier);
    }

    private DemandeFraisDto toDto(DemandeFrais d) {
        List<DemandeFraisDto.SuiviEtapeDto> suivi = new ArrayList<>();
        suivi.add(new DemandeFraisDto.SuiviEtapeDto("EMPLOYE", "EMPLOYÉ Soumission", "VALIDE", d.getDateSoumission().format(DATE_FORMAT)));
        String phaseManager = d.getStatut() == DemandeFrais.StatutDemande.EN_ATTENTE_MANAGER ? "EN_ATTENTE" : (d.getStatut() == DemandeFrais.StatutDemande.REFUSE ? "REFUSE" : "VALIDE");
        suivi.add(new DemandeFraisDto.SuiviEtapeDto("MANAGER", "MANAGER " + (d.getManagerNom() != null ? d.getManagerNom() : "Validation"), phaseManager, d.getManagerDecisionDate() != null ? d.getManagerDecisionDate().format(DATE_FORMAT) : "En attente"));
        String phaseRh = d.getStatut() == DemandeFrais.StatutDemande.EN_ATTENTE_COMPTABILITE ? "EN_ATTENTE" : (d.getStatut() == DemandeFrais.StatutDemande.VALIDE ? "VALIDE" : (d.getStatut() == DemandeFrais.StatutDemande.REFUSE ? "REFUSE" : "-"));
        suivi.add(new DemandeFraisDto.SuiviEtapeDto("RH", "RH " + (d.getRhNom() != null ? d.getRhNom() : "Validation Finale"), phaseRh, d.getRhDecisionDate() != null ? d.getRhDecisionDate().format(DATE_FORMAT) : "-"));

        String phaseActuelle = phaseToLabel(d.getStatut());
        return DemandeFraisDto.builder()
                .id(d.getId())
                .employeeId(d.getEmployeeId())
                .dossierId(d.getDossierId())
                .reference(d.getReference())
                .montant(d.getMontant())
                .categorie(d.getCategorie().name())
                .categorieLabel(categorieToLabel(d.getCategorie()))
                .modeTransport(d.getModeTransport() != null ? d.getModeTransport().name() : null)
                .kilometres(d.getKilometres())
                .ville(d.getVille())
                .anneesExperience(d.getAnneesExperience())
                .description(d.getDescription())
                .statut(d.getStatut().name())
                .statutLabel(statutToLabel(d.getStatut()))
                .dateSoumission(d.getDateSoumission().format(DATE_FORMAT))
                .dateSoumissionIso(d.getDateSoumission().toString())
                .dateRemboursement(d.getDateRemboursement() != null ? d.getDateRemboursement().format(DATE_FORMAT) : "-")
                .phaseActuelle(phaseActuelle)
                .managerNom(d.getManagerNom())
                .suivi(suivi)
                .build();
    }

    private DossierFraisDto toDossierDto(DossierFrais d) {
        List<DemandeFrais> notes = demandeRepository.findByDossierIdOrderByDateSoumissionAsc(d.getId());
        BigDecimal total = notes.stream().map(DemandeFrais::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
        return DossierFraisDto.builder()
                .id(d.getId())
                .employeeId(d.getEmployeeId())
                .titre(d.getTitre())
                .dateDebut(d.getDateDebut().format(DATE_FORMAT))
                .dateFin(d.getDateFin().format(DATE_FORMAT))
                .statut(d.getStatut().name())
                .statutLabel(statutDossierToLabel(d.getStatut()))
                .dateCreation(d.getDateCreation() != null ? d.getDateCreation().format(DATE_FORMAT) : "-")
                .dateCreationIso(d.getDateCreation() != null ? d.getDateCreation().toString() : null)
                .dateSoumission(d.getDateSoumission() != null ? d.getDateSoumission().format(DATE_FORMAT) : "-")
                .dateSoumissionIso(d.getDateSoumission() != null ? d.getDateSoumission().toString() : null)
                .managerNom(d.getManagerNom())
                .rhNom(d.getRhNom())
                .montantTotal(total)
                .notes(notes.stream().map(this::toDto).collect(Collectors.toList()))
                .build();
    }

    private static String phaseToLabel(DemandeFrais.StatutDemande s) {
        switch (s) {
            case EN_ATTENTE_MANAGER: return "Approbation Manager";
            case EN_ATTENTE_COMPTABILITE: return "Validation RH";
            case VALIDE: return "Validé RH";
            case REFUSE: return "Refusé";
            case REMBOURSE: return "Remboursé";
            default: return s.name();
        }
    }

    private static String categorieToLabel(DemandeFrais.CategorieFrais c) {
        switch (c) {
            case TRANSPORT: return "Transport";
            case REPAS: return "Repas";
            case HEBERGEMENT: return "Hébergement";
            case FOURNITURES: return "Fournitures";
            default: return "Autre";
        }
    }

    private static String statutToLabel(DemandeFrais.StatutDemande s) {
        switch (s) {
            case EN_ATTENTE_MANAGER: return "En attente Manager";
            case EN_ATTENTE_COMPTABILITE: return "En attente RH";
            case VALIDE: return "Validé RH";
            case REFUSE: return "Refusé";
            case REMBOURSE: return "Payé";
            default: return s.name();
        }
    }

    private static String statutDossierToLabel(DossierFrais.StatutDossier s) {
        switch (s) {
            case BROUILLON: return "Brouillon";
            case EN_ATTENTE_MANAGER: return "En attente Manager";
            case EN_ATTENTE_RH: return "En attente RH";
            case VALIDE: return "Validé";
            case REFUSE: return "Refusé";
            default: return s.name();
        }
    }
}
