package com.hub.conges.config;

import com.hub.conges.entity.DemandeConges;
import com.hub.conges.entity.ParametresConges;
import com.hub.conges.entity.SoldeConges;
import com.hub.conges.entity.ValidationConges;
import com.hub.conges.repository.DemandeCongesRepository;
import com.hub.conges.repository.ParametresCongesRepository;
import com.hub.conges.repository.SoldeCongesRepository;
import com.hub.conges.repository.ValidationCongesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final SoldeCongesRepository soldeRepository;
    private final DemandeCongesRepository demandeRepository;
    private final ValidationCongesRepository validationRepository;
    private final ParametresCongesRepository parametresCongesRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (parametresCongesRepository.count() == 0) {
            parametresCongesRepository.save(ParametresConges.builder()
                    .valeurAcquisitionMensuelle(new BigDecimal("1.50"))
                    .dateMiseAJour(LocalDateTime.now())
                    .creePar("SYSTEM")
                    .build());
            log.info("ParametresConges initialisés (1,5 j/mois)");
        }
        if (soldeRepository.count() > 0) {
            log.info("Conges data already present, skipping init");
            return;
        }
        long employeeId = 1L;
        int annee = LocalDate.now().getYear();
        LocalDateTime now = LocalDateTime.now();

        soldeRepository.save(SoldeConges.builder().employeeId(employeeId).type(SoldeConges.TypeSolde.ANNUEL).annee(annee).joursTotal(18).joursPris(0).dateDerniereMiseAJour(now).build());
        soldeRepository.save(SoldeConges.builder().employeeId(employeeId).type(SoldeConges.TypeSolde.RTT).annee(annee).joursTotal(4.5).joursPris(0).dateDerniereMiseAJour(now).build());
        soldeRepository.save(SoldeConges.builder().employeeId(employeeId).type(SoldeConges.TypeSolde.EXCEPTIONNEL).annee(annee).joursTotal(2).joursPris(0).dateDerniereMiseAJour(now).build());
        soldeRepository.save(SoldeConges.builder().employeeId(employeeId).type(SoldeConges.TypeSolde.MALADIE).annee(annee).joursTotal(0).joursPris(0).dateDerniereMiseAJour(now).build());

        DemandeConges d1 = DemandeConges.builder()
                .employeeId(employeeId)
                .dateDebut(LocalDate.of(2024, 5, 10))
                .dateFin(LocalDate.of(2024, 5, 12))
                .motif(DemandeConges.MotifAbsence.CONGES_ANNUELS)
                .periode(DemandeConges.PeriodeType.JOURNEE_COMPLETE)
                .statut(DemandeConges.StatutDemande.VALIDE)
                .dateSoumission(LocalDateTime.now().minusDays(30))
                .validateurNom("Sami El Fassi")
                .build();
        demandeRepository.save(d1);
        DemandeConges d2 = DemandeConges.builder()
                .employeeId(employeeId)
                .dateDebut(LocalDate.of(2024, 4, 24))
                .dateFin(LocalDate.of(2024, 4, 24))
                .motif(DemandeConges.MotifAbsence.RTT)
                .periode(DemandeConges.PeriodeType.JOURNEE_COMPLETE)
                .statut(DemandeConges.StatutDemande.VALIDE)
                .dateSoumission(LocalDateTime.now().minusDays(45))
                .validateurNom("Sami El Fassi")
                .build();
        demandeRepository.save(d2);
        DemandeConges d3 = DemandeConges.builder()
                .employeeId(employeeId)
                .dateDebut(LocalDate.of(2024, 3, 5))
                .dateFin(LocalDate.of(2024, 3, 6))
                .motif(DemandeConges.MotifAbsence.EVENEMENT_FAMILIAL)
                .periode(DemandeConges.PeriodeType.JOURNEE_COMPLETE)
                .statut(DemandeConges.StatutDemande.VALIDE)
                .dateSoumission(LocalDateTime.now().minusDays(60))
                .validateurNom("Sami El Fassi")
                .build();
        demandeRepository.save(d3);
        DemandeConges d4 = DemandeConges.builder()
                .employeeId(employeeId)
                .dateDebut(LocalDate.of(2024, 2, 15))
                .dateFin(LocalDate.of(2024, 2, 15))
                .motif(DemandeConges.MotifAbsence.RTT)
                .periode(DemandeConges.PeriodeType.DEMI_JOURNEE)
                .statut(DemandeConges.StatutDemande.EN_ATTENTE_MANAGER)
                .dateSoumission(LocalDateTime.now().minusDays(20))
                .build();
        demandeRepository.save(d4);
        DemandeConges d5 = DemandeConges.builder()
                .employeeId(employeeId)
                .dateDebut(LocalDate.of(2024, 1, 10))
                .dateFin(LocalDate.of(2024, 1, 15))
                .motif(DemandeConges.MotifAbsence.CONGES_ANNUELS)
                .periode(DemandeConges.PeriodeType.JOURNEE_COMPLETE)
                .statut(DemandeConges.StatutDemande.REFUSE)
                .dateSoumission(LocalDateTime.now().minusDays(80))
                .validateurNom("RH Centrale")
                .build();
        demandeRepository.save(d5);

        for (DemandeConges d : List.of(d1, d2, d3, d4, d5)) {
            validationRepository.save(ValidationConges.builder().demande(d).etape(ValidationConges.EtapeValidation.SOUMISSION).statut(ValidationConges.StatutValidation.VALIDE).dateValidation(d.getDateSoumission()).build());
            boolean managerOk = d.getStatut() != DemandeConges.StatutDemande.SOUMMIS && d.getStatut() != DemandeConges.StatutDemande.EN_ATTENTE_MANAGER;
            validationRepository.save(ValidationConges.builder().demande(d).etape(ValidationConges.EtapeValidation.VALIDATION_MANAGER).statut(managerOk ? ValidationConges.StatutValidation.VALIDE : ValidationConges.StatutValidation.EN_ATTENTE).dateValidation(managerOk ? d.getDateSoumission().plusHours(1) : null).build());
            boolean rhOk = d.getStatut() == DemandeConges.StatutDemande.VALIDE || d.getStatut() == DemandeConges.StatutDemande.REFUSE;
            validationRepository.save(ValidationConges.builder().demande(d).etape(ValidationConges.EtapeValidation.VALIDATION_RH).statut(rhOk ? (d.getStatut() == DemandeConges.StatutDemande.REFUSE ? ValidationConges.StatutValidation.REFUSE : ValidationConges.StatutValidation.VALIDE) : ValidationConges.StatutValidation.EN_ATTENTE).dateValidation(rhOk ? d.getDateSoumission().plusDays(1) : null).build());
        }

        log.info("Conges dev data initialized for employeeId={}", employeeId);
    }
}
