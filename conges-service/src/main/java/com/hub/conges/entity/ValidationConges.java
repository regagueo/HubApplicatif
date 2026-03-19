package com.hub.conges.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "validation_conges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationConges {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demande_id", nullable = false)
    private DemandeConges demande;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private EtapeValidation etape;

    @Column
    private Long validateurId;

    @Column(length = 128)
    private String validateurNom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private StatutValidation statut;

    @Column
    private LocalDateTime dateValidation;

    public enum EtapeValidation {
        SOUMISSION, VALIDATION_MANAGER, VALIDATION_RH
    }

    public enum StatutValidation {
        EN_ATTENTE, VALIDE, REFUSE
    }
}
