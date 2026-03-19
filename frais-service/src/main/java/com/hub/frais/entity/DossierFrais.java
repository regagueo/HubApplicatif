package com.hub.frais.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "dossier_frais")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DossierFrais {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long employeeId;

    @Column(nullable = false, length = 255)
    private String titre;

    @Column(nullable = false)
    private LocalDate dateDebut;

    @Column(nullable = false)
    private LocalDate dateFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private StatutDossier statut;

    @Column(nullable = false)
    private LocalDateTime dateCreation;

    @Column
    private LocalDateTime dateSoumission;

    @Column(length = 128)
    private String managerNom;

    @Column
    private LocalDateTime managerDecisionDate;

    @Column(length = 128)
    private String rhNom;

    @Column
    private LocalDateTime rhDecisionDate;

    public enum StatutDossier {
        BROUILLON,
        EN_ATTENTE_MANAGER,
        EN_ATTENTE_RH,
        VALIDE,
        REFUSE
    }
}
