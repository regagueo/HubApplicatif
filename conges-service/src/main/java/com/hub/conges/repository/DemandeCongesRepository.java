package com.hub.conges.repository;

import com.hub.conges.entity.DemandeConges;
import com.hub.conges.entity.DemandeConges.StatutDemande;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DemandeCongesRepository extends JpaRepository<DemandeConges, Long> {

    List<DemandeConges> findByEmployeeIdOrderByDateSoumissionDesc(Long employeeId);

    List<DemandeConges> findByStatutInOrderByDateSoumissionDesc(List<StatutDemande> statuts);
}
