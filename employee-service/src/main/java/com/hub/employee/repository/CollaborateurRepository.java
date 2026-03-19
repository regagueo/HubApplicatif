package com.hub.employee.repository;

import com.hub.employee.entity.Collaborateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CollaborateurRepository extends JpaRepository<Collaborateur, Long> {

    List<Collaborateur> findAllByOrderByNom();
}
