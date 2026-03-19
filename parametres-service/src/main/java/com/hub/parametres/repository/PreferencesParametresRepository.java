package com.hub.parametres.repository;

import com.hub.parametres.entity.PreferencesParametres;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PreferencesParametresRepository extends JpaRepository<PreferencesParametres, Long> {
    Optional<PreferencesParametres> findByUserId(Long userId);
}
