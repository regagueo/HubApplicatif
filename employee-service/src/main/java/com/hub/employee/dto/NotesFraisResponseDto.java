package com.hub.employee.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotesFraisResponseDto {
    private double enAttente;
    private double valide;
    private List<RemboursementDto> derniersRemboursements;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RemboursementDto {
        private Long id;
        private String libelle;
        private double montant;
    }
}
