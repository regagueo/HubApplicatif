package com.hub.frais.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "demande_frais")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandeFrais {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long employeeId;

    @Column
    private Long dossierId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CategorieFrais categorie;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private ModeTransport modeTransport;

    @Column(precision = 10, scale = 2)
    private BigDecimal kilometres;

    @Column(length = 128)
    private String ville;

    private Integer anneesExperience;

    @Column(nullable = false, length = 1024)
    private String description;

    /** ID du justificatif dans MongoDB GridFS */
    @Column(length = 64)
    private String justificatifFileId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private StatutDemande statut;

    @Column(nullable = false)
    private LocalDateTime dateSoumission;

    @Column(length = 32, unique = true)
    private String reference;

    @Column(length = 128)
    private String managerNom;

    private LocalDateTime managerDecisionDate;

    @Column(length = 128)
    private String rhNom;

    private LocalDateTime rhDecisionDate;

    private LocalDate dateRemboursement;

    public enum CategorieFrais {
        TRANSPORT, REPAS, HEBERGEMENT, FOURNITURES, AUTRE
    }

    public enum ModeTransport {
        VOITURE_PERSONNELLE,
        TRAIN_BUS_AVION,
        TAXI_COVOITURAGE,
        PEAGE_CARBURANT
    }

    public enum StatutDemande {
        EN_ATTENTE_MANAGER,
        EN_ATTENTE_COMPTABILITE,
        VALIDE,
        REFUSE,
        REMBOURSE
    }
}
