package com.hub.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MfaVerifyRequest {

    @NotBlank(message = "Temp token requis")
    private String tempToken;

    @NotBlank(message = "Code requis")
    @Pattern(regexp = "[0-9]{6}", message = "Le code doit contenir 6 chiffres")
    private String code;
}
