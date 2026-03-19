package com.hub.frais.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EncoursFraisDto {
    /** Montant total des demandes en attente (non remboursées, non refusées) */
    private BigDecimal montantEncours;
    /** Demandes en cours de validation */
    private List<DemandeFraisDto> demandesEncours;
}
