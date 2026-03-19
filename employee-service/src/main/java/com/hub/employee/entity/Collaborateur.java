package com.hub.employee.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "collaborateur")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Collaborateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String nom;

    @Column(nullable = false, length = 128)
    private String service;
}
