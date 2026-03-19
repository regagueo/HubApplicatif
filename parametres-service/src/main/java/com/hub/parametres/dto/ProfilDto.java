package com.hub.parametres.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfilDto {
    private String emailPro;
    private String telephone;
    private String photoUrl;
    private String poste;
    private String departement;
    private String localisation;
    private String typeContrat;
}
