package com.hub.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateDemandeCongesRequest {
    @NotBlank
    private String type;
    @NotNull
    private LocalDate dateDebut;
    @NotNull
    private LocalDate dateFin;
}
