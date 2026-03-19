package com.hub.frais.repository;

import com.hub.frais.entity.DossierFrais;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DossierFraisRepository extends JpaRepository<DossierFrais, Long> {
    List<DossierFrais> findByEmployeeIdOrderByDateCreationDesc(Long employeeId);
    List<DossierFrais> findByStatutInOrderByDateCreationDesc(List<DossierFrais.StatutDossier> statuts);
}
