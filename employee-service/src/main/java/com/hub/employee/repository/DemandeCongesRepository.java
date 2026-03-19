package com.hub.employee.repository;

import com.hub.employee.entity.DemandeConges;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DemandeCongesRepository extends JpaRepository<DemandeConges, Long> {

    List<DemandeConges> findByEmployeeIdOrderByDateDebutDesc(Long employeeId);
}
