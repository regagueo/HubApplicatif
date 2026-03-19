package com.hub.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MfaEnableRequest {

    @NotBlank
    private String secret;

    @NotBlank
    @Pattern(regexp = "[0-9]{6}", message = "Le code doit contenir 6 chiffres")
    private String code;
}
