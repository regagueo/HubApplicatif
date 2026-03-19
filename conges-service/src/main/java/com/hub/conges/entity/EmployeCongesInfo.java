package com.hub.conges.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "employe_conges_info", indexes = @Index(name = "idx_employe_conges_info_employe", columnList = "employe_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeCongesInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employe_id", nullable = false, unique = true)
    private Long employeId;

    @Column(name = "date_entree")
    private LocalDate dateEntree;
}
