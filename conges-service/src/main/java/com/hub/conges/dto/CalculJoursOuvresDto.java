package com.hub.conges.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalculJoursOuvresDto {
    private double joursOuvres;
    private int weekEndsExclus;
    private int joursFeriesExclus;
}
