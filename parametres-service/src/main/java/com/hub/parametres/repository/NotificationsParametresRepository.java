package com.hub.parametres.repository;

import com.hub.parametres.entity.NotificationsParametres;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationsParametresRepository extends JpaRepository<NotificationsParametres, Long> {
    Optional<NotificationsParametres> findByUserId(Long userId);
}
