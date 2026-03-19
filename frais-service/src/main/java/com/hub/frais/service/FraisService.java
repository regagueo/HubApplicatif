package com.hub.frais.service;

import com.hub.frais.dto.DemandeFraisDto;
import com.hub.frais.dto.EncoursFraisDto;
import com.hub.frais.entity.DemandeFrais;
import com.hub.frais.repository.DemandeFraisRepository;
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
    private final JustificatifStorageService justificatifStorage;

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
    public DemandeFraisDto createDemande(Long employeeId, BigDecimal montant, String categorie, String description, MultipartFile justificatif) {
        if (!getCurrentUser().getUserId().equals(employeeId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        DemandeFrais.CategorieFrais cat = DemandeFrais.CategorieFrais.valueOf(categorie);
        DemandeFrais entity = DemandeFrais.builder()
                .employeeId(employeeId)
                .montant(montant)
                .categorie(cat)
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
        return demandeRepository.findByEmployeeIdOrderByDateSoumissionDesc(employeeId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private DemandeFraisDto toDto(DemandeFrais d) {
        List<DemandeFraisDto.SuiviEtapeDto> suivi = new ArrayList<>();
        suivi.add(new DemandeFraisDto.SuiviEtapeDto("EMPLOYE", "EMPLOYÉ Soumission", "VALIDE", d.getDateSoumission().format(DATE_FORMAT)));
        String phaseManager = d.getStatut() == DemandeFrais.StatutDemande.EN_ATTENTE_MANAGER ? "EN_ATTENTE" : (d.getStatut() == DemandeFrais.StatutDemande.REFUSE ? "REFUSE" : "VALIDE");
        suivi.add(new DemandeFraisDto.SuiviEtapeDto("MANAGER", "MANAGER " + (d.getManagerNom() != null ? d.getManagerNom() : "Validation"), phaseManager, d.getManagerNom() != null ? d.getDateSoumission().format(DATE_FORMAT) : "En attente"));
        String phaseCompta = d.getStatut() == DemandeFrais.StatutDemande.REMBOURSE ? "VALIDE" : (d.getStatut() == DemandeFrais.StatutDemande.EN_ATTENTE_COMPTABILITE ? "EN_ATTENTE" : (d.getStatut() == DemandeFrais.StatutDemande.VALIDE ? "VALIDE" : "-"));
        suivi.add(new DemandeFraisDto.SuiviEtapeDto("COMPTABILITE", "COMPTABILITÉ Validation Finale", phaseCompta, d.getDateRemboursement() != null ? d.getDateRemboursement().format(DATE_FORMAT) : "-"));

        String phaseActuelle = phaseToLabel(d.getStatut());
        return DemandeFraisDto.builder()
                .id(d.getId())
                .employeeId(d.getEmployeeId())
                .reference(d.getReference())
                .montant(d.getMontant())
                .categorie(d.getCategorie().name())
                .categorieLabel(categorieToLabel(d.getCategorie()))
                .description(d.getDescription())
                .statut(d.getStatut().name())
                .statutLabel(statutToLabel(d.getStatut()))
                .dateSoumission(d.getDateSoumission().format(DATE_FORMAT))
                .dateRemboursement(d.getDateRemboursement() != null ? d.getDateRemboursement().format(DATE_FORMAT) : "-")
                .phaseActuelle(phaseActuelle)
                .managerNom(d.getManagerNom())
                .suivi(suivi)
                .build();
    }

    private static String phaseToLabel(DemandeFrais.StatutDemande s) {
        switch (s) {
            case EN_ATTENTE_MANAGER: return "Approbation Manager";
            case EN_ATTENTE_COMPTABILITE: return "Validation Comptabilité";
            case VALIDE: return "Validé";
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
            case EN_ATTENTE_COMPTABILITE: return "En attente Comptabilité";
            case VALIDE: return "Validé";
            case REFUSE: return "Refusé";
            case REMBOURSE: return "Payé";
            default: return s.name();
        }
    }
}
