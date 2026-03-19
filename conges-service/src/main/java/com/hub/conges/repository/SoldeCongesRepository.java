package com.hub.conges.repository;

import com.hub.conges.entity.SoldeConges;
import com.hub.conges.entity.SoldeConges.TypeSolde;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SoldeCongesRepository extends JpaRepository<SoldeConges, Long> {

    List<SoldeConges> findByEmployeeIdAndAnnee(Long employeeId, int annee);

    Optional<SoldeConges> findByEmployeeIdAndAnneeAndType(Long employeeId, int annee, TypeSolde type);

    @Query("SELECT DISTINCT s.employeeId FROM SoldeConges s WHERE s.annee = :annee")
    List<Long> findDistinctEmployeeIdsByAnnee(@Param("annee") int annee);
}
