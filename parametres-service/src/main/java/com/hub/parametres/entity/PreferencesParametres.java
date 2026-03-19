package com.hub.parametres.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "parametres_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreferencesParametres {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(length = 8, nullable = false)
    @Builder.Default
    private String language = "fr";

    @Column(length = 16, nullable = false)
    @Builder.Default
    private String theme = "light";
}
