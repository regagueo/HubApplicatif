package com.hub.conges.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoldeCongesDto {
    private String type;       // ANNUEL, RTT, EXCEPTIONNEL, MALADIE
    private String label;      // Congés Annuels, RTT, etc.
    private double jours;
    private double joursPris;
    private double joursRestants;
}
