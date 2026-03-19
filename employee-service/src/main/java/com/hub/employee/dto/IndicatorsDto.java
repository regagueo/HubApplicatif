package com.hub.employee.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndicatorsDto {
    private int joursPris;
    private int joursRestants;
    private int totalJours;
    private Double notesFraisEnAttente;  // montant total en attente
    private Integer delaiMoyenJours;     // délai moyen (ex. traitement)
}
