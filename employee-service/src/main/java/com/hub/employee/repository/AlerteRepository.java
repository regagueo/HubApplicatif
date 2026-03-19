package com.hub.employee.repository;

import com.hub.employee.entity.Alerte;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlerteRepository extends JpaRepository<Alerte, Long> {

    List<Alerte> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);
}
