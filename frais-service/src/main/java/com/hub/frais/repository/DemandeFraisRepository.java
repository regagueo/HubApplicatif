package com.hub.frais.repository;

import com.hub.frais.entity.DemandeFrais;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DemandeFraisRepository extends JpaRepository<DemandeFrais, Long> {

    List<DemandeFrais> findByEmployeeIdOrderByDateSoumissionDesc(Long employeeId);

    List<DemandeFrais> findByEmployeeIdAndStatutInOrderByDateSoumissionDesc(Long employeeId, List<DemandeFrais.StatutDemande> statuts);

    long countByEmployeeIdAndStatutIn(Long employeeId, List<DemandeFrais.StatutDemande> statuts);
}
