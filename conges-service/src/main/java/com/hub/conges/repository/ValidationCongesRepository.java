package com.hub.conges.repository;

import com.hub.conges.entity.ValidationConges;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ValidationCongesRepository extends JpaRepository<ValidationConges, Long> {

    List<ValidationConges> findByDemandeIdOrderByEtape(Long demandeId);
}
