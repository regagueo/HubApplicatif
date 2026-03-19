package com.hub.parametres.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "parametres_securite")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecuriteParametres {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false)
    @Builder.Default
    private Boolean mfaEnabled = false;

    private Instant passwordChangedAt;
}
