package com.hub.conges.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandeCongesDto {
    private Long id;
    private Long employeeId;
    private String dateDebut;
    private String dateFin;
    private String motif;
    private String motifCode;
    private String periode;
    private String commentaire;
    private String statut;
    private String statutLabel;
    private String dateSoumission;
    private double dureeJours;
    private String validateurNom;
    private List<SuiviEtapeDto> suivi;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SuiviEtapeDto {
        private String etape;
        private String label;
        private String statut;  // VALIDE, EN_ATTENTE
        private String date;
    }
}
