package com.hub.employee.repository;

import com.hub.employee.entity.NoteFrais;
import com.hub.employee.entity.NoteFrais.StatutFrais;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoteFraisRepository extends JpaRepository<NoteFrais, Long> {

    List<NoteFrais> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);

    List<NoteFrais> findByEmployeeIdAndStatut(Long employeeId, StatutFrais statut);
}
