package com.hub.conges.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "solde_conges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoldeConges {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long employeeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TypeSolde type;

    @Column(nullable = false)
    private int annee;

    @Column(nullable = false)
    private double joursTotal;

    @Column(nullable = false)
    private double joursPris;

    @Column(name = "date_derniere_mise_a_jour")
    private LocalDateTime dateDerniereMiseAJour;

    public enum TypeSolde {
        ANNUEL, RTT, EXCEPTIONNEL, MALADIE
    }

    public double getJoursRestants() {
        return Math.max(0, joursTotal - joursPris);
    }
}
