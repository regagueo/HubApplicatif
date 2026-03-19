package com.hub.employee.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateNoteFraisRequest {
    @NotBlank
    private String libelle;
    @NotNull
    @DecimalMin("0")
    private BigDecimal montant;
}
