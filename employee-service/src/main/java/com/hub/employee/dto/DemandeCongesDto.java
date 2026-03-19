package com.hub.employee.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandeCongesDto {
    private Long id;
    private String type;
    private String dateDebut;
    private String dateFin;
    private String statut; // EN_ATTENTE, VALIDE, REJETE
}
