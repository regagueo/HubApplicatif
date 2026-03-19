package com.hub.conges.repository;

import com.hub.conges.entity.JoursFeries;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface JoursFeriesRepository extends JpaRepository<JoursFeries, Long> {

    List<JoursFeries> findByAnneeOrderByDateAsc(int annee);

    Optional<JoursFeries> findByDateAndAnnee(LocalDate date, int annee);

    boolean existsByDateAndAnnee(LocalDate date, int annee);
}
