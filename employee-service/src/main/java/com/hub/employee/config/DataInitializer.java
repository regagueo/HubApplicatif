package com.hub.employee.config;

import com.hub.employee.entity.Alerte;
import com.hub.employee.entity.Collaborateur;
import com.hub.employee.entity.DemandeConges;
import com.hub.employee.entity.NoteFrais;
import com.hub.employee.repository.AlerteRepository;
import com.hub.employee.repository.CollaborateurRepository;
import com.hub.employee.repository.DemandeCongesRepository;
import com.hub.employee.repository.NoteFraisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final DemandeCongesRepository congesRepository;
    private final NoteFraisRepository noteFraisRepository;
    private final AlerteRepository alerteRepository;
    private final CollaborateurRepository collaborateurRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (congesRepository.count() > 0) {
            log.info("Data already present, skipping init");
            return;
        }

        long employeeId = 1L;

        congesRepository.save(DemandeConges.builder()
                .employeeId(employeeId)
                .type("Congés Payés")
                .dateDebut(LocalDate.of(2025, 6, 12))
                .dateFin(LocalDate.of(2025, 6, 15))
                .statut(DemandeConges.StatutConges.EN_ATTENTE)
                .build());
        congesRepository.save(DemandeConges.builder()
                .employeeId(employeeId)
                .type("RTT")
                .dateDebut(LocalDate.of(2025, 5, 5))
                .dateFin(LocalDate.of(2025, 5, 5))
                .statut(DemandeConges.StatutConges.VALIDE)
                .build());
        congesRepository.save(DemandeConges.builder()
                .employeeId(employeeId)
                .type("Congé Maladie")
                .dateDebut(LocalDate.of(2025, 4, 12))
                .dateFin(LocalDate.of(2025, 4, 14))
                .statut(DemandeConges.StatutConges.REJETE)
                .build());
        congesRepository.save(DemandeConges.builder()
                .employeeId(employeeId)
                .type("Congés Payés")
                .dateDebut(LocalDate.of(2025, 3, 1))
                .dateFin(LocalDate.of(2025, 3, 2))
                .statut(DemandeConges.StatutConges.VALIDE)
                .build());

        noteFraisRepository.save(NoteFrais.builder()
                .employeeId(employeeId)
                .libelle("Déplacement en attente")
                .montant(new BigDecimal("1450.00"))
                .statut(NoteFrais.StatutFrais.EN_ATTENTE)
                .createdAt(Instant.now())
                .build());
        noteFraisRepository.save(NoteFrais.builder()
                .employeeId(employeeId)
                .libelle("Déplacement Casablanca")
                .montant(new BigDecimal("450.00"))
                .statut(NoteFrais.StatutFrais.VALIDE)
                .createdAt(Instant.now().minusSeconds(86400 * 5))
                .build());
        noteFraisRepository.save(NoteFrais.builder()
                .employeeId(employeeId)
                .libelle("Fournitures de bureau")
                .montant(new BigDecimal("120.00"))
                .statut(NoteFrais.StatutFrais.VALIDE)
                .createdAt(Instant.now().minusSeconds(86400 * 10))
                .build());

        alerteRepository.save(Alerte.builder()
                .employeeId(employeeId)
                .type("conges_valide")
                .titre("Congé Validé")
                .message("Votre demande pour Juin a été validée par le Manager.")
                .createdAt(Instant.now().minusSeconds(7200))
                .build());
        alerteRepository.save(Alerte.builder()
                .employeeId(employeeId)
                .type("justificatif_manquant")
                .titre("Note de frais")
                .message("Veuillez joindre le justificatif manquant pour l'invitation client.")
                .createdAt(Instant.now().minusSeconds(86400))
                .build());
        alerteRepository.save(Alerte.builder()
                .employeeId(employeeId)
                .type("document_rh")
                .titre("Document RH")
                .message("Votre bulletin de paie de Mai est disponible.")
                .createdAt(Instant.now().minusSeconds(86400 * 2))
                .build());

        collaborateurRepository.save(Collaborateur.builder().nom("Youssef Alami").service("Manager RH").build());
        collaborateurRepository.save(Collaborateur.builder().nom("Salma Mansouri").service("Comptabilité").build());
        collaborateurRepository.save(Collaborateur.builder().nom("Driss Tazi").service("IT Support").build());
        collaborateurRepository.save(Collaborateur.builder().nom("Leila Benani").service("Marketing").build());

        log.info("Employee dev data initialized for employeeId={}", employeeId);
    }
}
