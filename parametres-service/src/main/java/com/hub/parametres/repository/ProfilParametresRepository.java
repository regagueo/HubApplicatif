package com.hub.parametres.repository;

import com.hub.parametres.entity.ProfilParametres;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProfilParametresRepository extends JpaRepository<ProfilParametres, Long> {
    Optional<ProfilParametres> findByUserId(Long userId);
}
