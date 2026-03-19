package com.hub.conges.controller;

import com.hub.conges.dto.*;
import com.hub.conges.service.AcquisitionCongesService;
import com.hub.conges.service.CongesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping({"/api/conges", "/conges"})
@RequiredArgsConstructor
public class CongesController {

    private final CongesService congesService;
    private final AcquisitionCongesService acquisitionCongesService;

    @GetMapping("/{employeeId}/soldes")
    public ResponseEntity<List<SoldeCongesDto>> getSoldes(@PathVariable Long employeeId) {
        return ResponseEntity.ok(congesService.getSoldes(employeeId));
    }

    @PostMapping("/{employeeId}/demandes")
    public ResponseEntity<DemandeCongesDto> createDemande(
            @PathVariable Long employeeId,
            @Valid @RequestBody CreateDemandeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(congesService.createDemande(employeeId, request));
    }

    @GetMapping("/{employeeId}/demandes")
    public ResponseEntity<List<DemandeCongesDto>> getDemandes(
            @PathVariable Long employeeId,
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false) String statut) {
        return ResponseEntity.ok(congesService.getDemandes(employeeId, annee, statut));
    }

    @GetMapping("/demandes/{demandeId}")
    public ResponseEntity<DemandeCongesDto> getDemande(@PathVariable Long demandeId) {
        return ResponseEntity.ok(congesService.getDemande(demandeId));
    }

    @GetMapping("/demandes/{demandeId}/suivi")
    public ResponseEntity<List<DemandeCongesDto.SuiviEtapeDto>> getSuivi(@PathVariable Long demandeId) {
        return ResponseEntity.ok(congesService.getSuivi(demandeId));
    }

    @PutMapping("/{employeeId}/demandes/{demandeId}")
    public ResponseEntity<DemandeCongesDto> updateDemande(
            @PathVariable Long employeeId,
            @PathVariable Long demandeId,
            @Valid @RequestBody CreateDemandeRequest request) {
        return ResponseEntity.ok(congesService.updateDemande(employeeId, demandeId, request));
    }

    @DeleteMapping("/{employeeId}/demandes/{demandeId}")
    public ResponseEntity<Void> annulerDemande(
            @PathVariable Long employeeId,
            @PathVariable Long demandeId) {
        congesService.annulerDemande(employeeId, demandeId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('MANAGER') or hasRole('RH') or hasRole('ADMIN')")
    @GetMapping("/demandes/en-attente")
    public ResponseEntity<List<DemandeCongesDto>> getDemandesEnAttente(@RequestParam(defaultValue = "MANAGER") String etape) {
        return ResponseEntity.ok(congesService.getDemandesEnAttente(etape));
    }

    @PreAuthorize("hasRole('MANAGER') or hasRole('RH') or hasRole('ADMIN')")
    @GetMapping("/demandes/validation")
    public ResponseEntity<List<DemandeCongesDto>> getDemandesValidation(@RequestParam(defaultValue = "MANAGER") String etape) {
        return ResponseEntity.ok(congesService.getDemandesValidation(etape));
    }

    @PreAuthorize("hasRole('MANAGER') or hasRole('RH') or hasRole('ADMIN')")
    @PutMapping("/demandes/{demandeId}/valider-manager")
    public ResponseEntity<DemandeCongesDto> validerParManager(
            @PathVariable Long demandeId,
            @RequestParam(required = false) String validateurNom) {
        return ResponseEntity.ok(congesService.validerParManager(demandeId, validateurNom));
    }

    @PreAuthorize("hasRole('MANAGER') or hasRole('RH') or hasRole('ADMIN')")
    @PutMapping("/demandes/{demandeId}/refuser-manager")
    public ResponseEntity<DemandeCongesDto> refuserParManager(
            @PathVariable Long demandeId,
            @RequestParam(required = false) String validateurNom) {
        return ResponseEntity.ok(congesService.refuserParManager(demandeId, validateurNom));
    }

    @PreAuthorize("hasRole('RH') or hasRole('ADMIN')")
    @PutMapping("/demandes/{demandeId}/valider-rh")
    public ResponseEntity<DemandeCongesDto> validerParRH(
            @PathVariable Long demandeId,
            @RequestParam(required = false) String validateurNom) {
        return ResponseEntity.ok(congesService.validerParRH(demandeId, validateurNom));
    }

    @PreAuthorize("hasRole('RH') or hasRole('ADMIN')")
    @PutMapping("/demandes/{demandeId}/refuser-rh")
    public ResponseEntity<DemandeCongesDto> refuserParRH(
            @PathVariable Long demandeId,
            @RequestParam(required = false) String validateurNom) {
        return ResponseEntity.ok(congesService.refuserParRH(demandeId, validateurNom));
    }

    @GetMapping("/{employeeId}/historique-solde")
    public ResponseEntity<List<HistoriqueSoldeDto>> getHistoriqueSolde(
            @PathVariable Long employeeId,
            @RequestParam(required = false) Integer annee) {
        return ResponseEntity.ok(congesService.getHistoriqueSolde(employeeId, annee));
    }

    @PreAuthorize("hasRole('RH') or hasRole('ADMIN')")
    @GetMapping("/rh/soldes-departement")
    public ResponseEntity<List<SoldeEmployeDto>> getAllSoldesForRh(
            @RequestParam(required = false) Integer annee) {
        return ResponseEntity.ok(congesService.getAllSoldesForRh(annee));
    }

    @GetMapping("/calcul-jours-ouvres")
    public ResponseEntity<CalculJoursOuvresDto> calculJoursOuvres(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(defaultValue = "JOURNEE_COMPLETE") String periode) {
        return ResponseEntity.ok(congesService.calculJoursOuvres(dateDebut, dateFin, periode));
    }

    @PreAuthorize("hasRole('RH') or hasRole('ADMIN')")
    @PostMapping("/employes/{employeeId}/initialiser-solde")
    public ResponseEntity<Void> initialiserSolde(
            @PathVariable Long employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateEntree) {
        acquisitionCongesService.initialiserSoldePourEmploye(employeeId, dateEntree);
        return ResponseEntity.ok().build();
    }
}
