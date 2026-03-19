package com.hub.parametres.repository;

import com.hub.parametres.entity.SecuriteParametres;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SecuriteParametresRepository extends JpaRepository<SecuriteParametres, Long> {
    Optional<SecuriteParametres> findByUserId(Long userId);
}
