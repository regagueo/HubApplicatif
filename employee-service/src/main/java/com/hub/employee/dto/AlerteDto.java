package com.hub.employee.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlerteDto {
    private Long id;
    private String type;   // conges_valide, justificatif_manquant, document_rh
    private String titre;
    private String message;
    private String date;   // "IL Y A 2H", "HIER", "2 JOURS"
}
