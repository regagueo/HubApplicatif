package com.hub.conges.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "historique_solde", indexes = {
    @Index(name = "idx_historique_employe_annee", columnList = "employe_id, annee"),
    @Index(name = "idx_historique_date", columnList = "date_mouvement")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoriqueSolde {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employe_id", nullable = false)
    private Long employeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_mouvement", nullable = false, length = 32)
    private TypeMouvement typeMouvement;

    @Column(nullable = false)
    private double valeur;

    @Column(name = "date_mouvement", nullable = false)
    private LocalDateTime dateMouvement;

    @Column(name = "reference_demande_id")
    private Long referenceDemandeId;

    @Column(nullable = false)
    private int annee;

    @Column(length = 512)
    private String libelle;

    public enum TypeMouvement {
        ACQUISITION_MENSUELLE,
        CONSOMMATION,
        ACQUISITION_PRORATA,
        AJUSTEMENT_MANUAL
    }
}
