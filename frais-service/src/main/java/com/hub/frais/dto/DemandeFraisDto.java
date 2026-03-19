package com.hub.frais.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandeFraisDto {
    private Long id;
    private Long employeeId;
    private String reference;
    private BigDecimal montant;
    private String categorie;
    private String categorieLabel;
    private String description;
    private String statut;
    private String statutLabel;
    private String dateSoumission;
    private String dateRemboursement; // "-" si non remboursé
    private String phaseActuelle;     // pour affichage workflow
    private String managerNom;
    /** Étapes du workflow pour affichage */
    private List<SuiviEtapeDto> suivi;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SuiviEtapeDto {
        private String code;   // EMPLOYE, MANAGER, COMPTABILITE
        private String label;
        private String statut; // VALIDE, EN_ATTENTE
        private String date;
    }
}
