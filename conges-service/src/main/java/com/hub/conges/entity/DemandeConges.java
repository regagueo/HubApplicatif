package com.hub.conges.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "demande_conges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandeConges {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long employeeId;

    @Column(nullable = false)
    private LocalDate dateDebut;

    @Column(nullable = false)
    private LocalDate dateFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MotifAbsence motif;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private PeriodeType periode;

    @Column(length = 1024)
    private String commentaire;

    @Column(name = "duree_jours_ouvres")
    private Double dureeJoursOuvres;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private StatutDemande statut;

    @Column(nullable = false)
    private LocalDateTime dateSoumission;

    @Column(length = 128)
    private String validateurNom;

    @OneToMany(mappedBy = "demande", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ValidationConges> validations = new ArrayList<>();

    public enum MotifAbsence {
        CONGES_ANNUELS, RTT, EVENEMENT_FAMILIAL, MALADIE, CONGES_EXCEPTIONNELS, AUTRE
    }

    public enum PeriodeType {
        JOURNEE_COMPLETE, DEMI_JOURNEE
    }

    public enum StatutDemande {
        SOUMMIS, EN_ATTENTE_MANAGER, EN_ATTENTE_RH, VALIDE, REFUSE
    }
}
