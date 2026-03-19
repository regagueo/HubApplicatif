package com.hub.conges.controller;

import com.hub.conges.entity.JoursFeries;
import com.hub.conges.service.JoursFeriesService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping({"/api/conges", "/conges"})
@RequiredArgsConstructor
public class JoursFeriesController {

    private final JoursFeriesService joursFeriesService;

    @GetMapping("/jours-feries")
    public ResponseEntity<List<JoursFeries>> listByAnnee(@RequestParam int annee) {
        return ResponseEntity.ok(joursFeriesService.listByAnnee(annee));
    }

    @PreAuthorize("hasRole('RH') or hasRole('ADMIN')")
    @PostMapping("/jours-feries/sync")
    public ResponseEntity<Integer> syncFromNager(@RequestParam int annee) {
        return ResponseEntity.ok(joursFeriesService.syncAnneeFromNager(annee));
    }

    @PreAuthorize("hasRole('RH') or hasRole('ADMIN')")
    @PostMapping("/jours-feries")
    public ResponseEntity<JoursFeries> ajouterManuel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String libelle) {
        return ResponseEntity.ok(joursFeriesService.ajouterManuel(date, libelle));
    }

    @PreAuthorize("hasRole('RH') or hasRole('ADMIN')")
    @DeleteMapping("/jours-feries/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        joursFeriesService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
