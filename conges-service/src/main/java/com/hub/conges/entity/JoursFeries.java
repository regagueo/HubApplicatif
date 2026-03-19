package com.hub.conges.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "jours_feries", indexes = {
    @Index(name = "idx_jours_feries_annee", columnList = "annee"),
    @Index(name = "idx_jours_feries_date", columnList = "date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JoursFeries {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @Column(length = 256)
    private String libelle;

    @Column(nullable = false)
    private int annee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private SourceJoursFeries source;

    public enum SourceJoursFeries {
        NAGER_API,
        MANUEL
    }
}
