package com.hub.employee.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "note_frais")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoteFrais {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long employeeId;

    @Column(nullable = false, length = 255)
    private String libelle;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutFrais statut;

    @Column(nullable = false)
    private Instant createdAt;

    public enum StatutFrais {
        EN_ATTENTE, VALIDE
    }
}
