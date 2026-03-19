package com.hub.conges.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "parametres_conges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParametresConges {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "valeur_acquisition_mensuelle", nullable = false, precision = 4, scale = 2)
    private BigDecimal valeurAcquisitionMensuelle;

    @Column(name = "date_mise_a_jour", nullable = false)
    private LocalDateTime dateMiseAJour;

    @Column(name = "cree_par", length = 128)
    private String creePar;
}
