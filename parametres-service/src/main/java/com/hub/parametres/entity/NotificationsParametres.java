package com.hub.parametres.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "parametres_notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationsParametres {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false)
    @Builder.Default
    private Boolean emailAlerts = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean pushEnabled = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean smsEnabled = false;
}
