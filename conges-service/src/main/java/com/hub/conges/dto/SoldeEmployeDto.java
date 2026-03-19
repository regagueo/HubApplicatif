package com.hub.conges.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoldeEmployeDto {
    private Long employeeId;
    private String type;
    private String label;
    private double joursTotal;
    private double joursPris;
    private double joursRestants;
    private int annee;
}
