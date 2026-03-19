package com.hub.parametres.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "parametres_profil")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfilParametres {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(length = 255)
    private String emailPro;

    @Column(length = 32)
    private String telephone;

    @Column(length = 512)
    private String photoUrl;

    @Column(length = 128)
    private String poste;

    @Column(length = 128)
    private String departement;

    @Column(length = 128)
    private String localisation;

    @Column(length = 64)
    private String typeContrat;
}
