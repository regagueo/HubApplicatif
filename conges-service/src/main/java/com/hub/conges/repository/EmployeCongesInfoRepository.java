package com.hub.conges.repository;

import com.hub.conges.entity.EmployeCongesInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployeCongesInfoRepository extends JpaRepository<EmployeCongesInfo, Long> {

    Optional<EmployeCongesInfo> findByEmployeId(Long employeId);
}
