package com.hub.conges.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoriqueSoldeDto {
    private Long id;
    private String typeMouvement;
    private double valeur;
    private LocalDateTime dateMouvement;
    private Long referenceDemandeId;
    private String libelle;
}
