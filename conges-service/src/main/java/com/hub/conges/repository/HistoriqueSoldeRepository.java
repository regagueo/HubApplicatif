package com.hub.conges.repository;

import com.hub.conges.entity.HistoriqueSolde;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoriqueSoldeRepository extends JpaRepository<HistoriqueSolde, Long> {

    List<HistoriqueSolde> findByEmployeIdOrderByDateMouvementDesc(Long employeId, Pageable pageable);

    List<HistoriqueSolde> findByEmployeIdAndAnneeOrderByDateMouvementDesc(Long employeId, int annee);
}
