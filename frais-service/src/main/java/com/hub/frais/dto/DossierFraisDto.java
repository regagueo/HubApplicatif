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
public class DossierFraisDto {
    private Long id;
    private Long employeeId;
    private String titre;
    private String dateDebut;
    private String dateFin;
    private String statut;
    private String statutLabel;
    private String dateCreation;
    private String dateCreationIso;
    private String dateSoumission;
    private String dateSoumissionIso;
    private String managerNom;
    private String rhNom;
    private BigDecimal montantTotal;
    private List<DemandeFraisDto> notes;
}
