package com.hub.conges.controller;

import com.hub.conges.entity.ParametresConges;
import com.hub.conges.service.ParametresCongesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping({"/api/conges", "/conges"})
@RequiredArgsConstructor
public class ParametresCongesController {

    private final ParametresCongesService parametresCongesService;

    @GetMapping("/parametres")
    public ResponseEntity<ParametresConges> getParametres() {
        return ResponseEntity.ok(parametresCongesService.getParametres());
    }

    @PreAuthorize("hasRole('RH') or hasRole('ADMIN')")
    @PutMapping("/parametres")
    public ResponseEntity<ParametresConges> mettreAJour(
            @RequestParam BigDecimal valeurAcquisitionMensuelle,
            @RequestParam(required = false) String creePar) {
        return ResponseEntity.ok(parametresCongesService.mettreAJour(valeurAcquisitionMensuelle, creePar));
    }
}
