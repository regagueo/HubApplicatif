package com.hub.conges.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateDemandeRequest {
    @NotNull
    private LocalDate dateDebut;
    @NotNull
    private LocalDate dateFin;
    @NotBlank
    private String motif;   // CONGES_ANNUELS, RTT, etc.
    @NotBlank
    private String periode; // JOURNEE_COMPLETE, DEMI_JOURNEE
    private String commentaire;
}
