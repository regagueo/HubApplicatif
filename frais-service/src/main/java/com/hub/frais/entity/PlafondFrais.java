package com.hub.frais.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "plafonds_frais")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlafondFrais {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal transportTarifKm;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal taxiForfaitSansJustificatif;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal repasMoins3Ans;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal repas3AnsEtPlus;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal repas3AnsEtPlusMax;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal hebergementCasablanca;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal hebergementAutresVilles;

    @Column(nullable = false)
    private Boolean perDiemActif;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal perDiemGlobal;

    @Column(nullable = false)
    private LocalDateTime dateMiseAJour;

    @Column(length = 128)
    private String modifiePar;
}
