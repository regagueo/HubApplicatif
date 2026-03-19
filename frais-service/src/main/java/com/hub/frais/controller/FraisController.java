package com.hub.frais.controller;

import com.hub.frais.dto.DemandeFraisDto;
import com.hub.frais.dto.DossierFraisDto;
import com.hub.frais.dto.EncoursFraisDto;
import com.hub.frais.service.FraisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping({"/api/frais", "/frais"})
@RequiredArgsConstructor
public class FraisController {

    private final FraisService fraisService;

    @PostMapping(value = "/{employeeId}/demande", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DemandeFraisDto> createDemande(
            @PathVariable Long employeeId,
            @RequestParam("montant") BigDecimal montant,
            @RequestParam("categorie") String categorie,
            @RequestParam(value = "modeTransport", required = false) String modeTransport,
            @RequestParam(value = "kilometres", required = false) BigDecimal kilometres,
            @RequestParam(value = "ville", required = false) String ville,
            @RequestParam(value = "anneesExperience", required = false) Integer anneesExperience,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "justificatif", required = false) MultipartFile justificatif) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                fraisService.createDemande(
                        employeeId,
                        montant,
                        categorie,
                        modeTransport,
                        kilometres,
                        ville,
                        anneesExperience,
                        description,
                        justificatif
                )
        );
    }

    @PostMapping("/{employeeId}/dossiers")
    public ResponseEntity<DossierFraisDto> createDossier(
            @PathVariable Long employeeId,
            @RequestParam String titre,
            @RequestParam LocalDate dateDebut,
            @RequestParam LocalDate dateFin) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fraisService.createDossier(employeeId, titre, dateDebut, dateFin));
    }

    @PostMapping(value = "/{employeeId}/dossiers/{dossierId}/notes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DemandeFraisDto> addNoteToDossier(
            @PathVariable Long employeeId,
            @PathVariable Long dossierId,
            @RequestParam("montant") BigDecimal montant,
            @RequestParam("categorie") String categorie,
            @RequestParam(value = "modeTransport", required = false) String modeTransport,
            @RequestParam(value = "kilometres", required = false) BigDecimal kilometres,
            @RequestParam(value = "ville", required = false) String ville,
            @RequestParam(value = "anneesExperience", required = false) Integer anneesExperience,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "justificatif", required = false) MultipartFile justificatif) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                fraisService.createDemandeDansDossier(employeeId, dossierId, montant, categorie, modeTransport, kilometres, ville, anneesExperience, description, justificatif)
        );
    }

    @PutMapping("/{employeeId}/dossiers/{dossierId}/soumettre")
    public ResponseEntity<DossierFraisDto> soumettreDossier(
            @PathVariable Long employeeId,
            @PathVariable Long dossierId) {
        return ResponseEntity.ok(fraisService.soumettreDossier(employeeId, dossierId));
    }

    @GetMapping("/{employeeId}/dossiers")
    public ResponseEntity<List<DossierFraisDto>> getHistoriqueDossiers(@PathVariable Long employeeId) {
        return ResponseEntity.ok(fraisService.getHistoriqueDossiers(employeeId));
    }

    @GetMapping("/dossiers/{dossierId}")
    public ResponseEntity<DossierFraisDto> getDossier(@PathVariable Long dossierId) {
        return ResponseEntity.ok(fraisService.getDossier(dossierId));
    }

    @GetMapping("/{employeeId}/encours")
    public ResponseEntity<EncoursFraisDto> getEncours(@PathVariable Long employeeId) {
        return ResponseEntity.ok(fraisService.getEncours(employeeId));
    }

    @GetMapping("/{employeeId}/historique")
    public ResponseEntity<List<DemandeFraisDto>> getHistorique(@PathVariable Long employeeId) {
        return ResponseEntity.ok(fraisService.getHistorique(employeeId));
    }

    @PreAuthorize("hasRole('MANAGER') or hasRole('RH') or hasRole('ADMIN')")
    @GetMapping("/demandes/validation")
    public ResponseEntity<List<DemandeFraisDto>> getDemandesValidation(@RequestParam(defaultValue = "MANAGER") String etape) {
        return ResponseEntity.ok(fraisService.getDemandesValidation(etape));
    }

    @PreAuthorize("hasRole('MANAGER') or hasRole('RH') or hasRole('ADMIN')")
    @GetMapping("/dossiers/validation")
    public ResponseEntity<List<DossierFraisDto>> getDossiersValidation(@RequestParam(defaultValue = "MANAGER") String etape) {
        return ResponseEntity.ok(fraisService.getDossiersValidation(etape));
    }

    @PreAuthorize("hasRole('MANAGER') or hasRole('RH') or hasRole('ADMIN')")
    @PutMapping("/demandes/{demandeId}/valider-manager")
    public ResponseEntity<DemandeFraisDto> validerParManager(
            @PathVariable Long demandeId,
            @RequestParam(required = false) String validateurNom) {
        return ResponseEntity.ok(fraisService.validerParManager(demandeId, validateurNom));
    }

    @PreAuthorize("hasRole('MANAGER') or hasRole('RH') or hasRole('ADMIN')")
    @PutMapping("/demandes/{demandeId}/refuser-manager")
    public ResponseEntity<DemandeFraisDto> refuserParManager(
            @PathVariable Long demandeId,
            @RequestParam(required = false) String validateurNom) {
        return ResponseEntity.ok(fraisService.refuserParManager(demandeId, validateurNom));
    }

    @PreAuthorize("hasRole('MANAGER') or hasRole('RH') or hasRole('ADMIN')")
    @PutMapping("/dossiers/{dossierId}/valider-manager")
    public ResponseEntity<DossierFraisDto> validerDossierParManager(
            @PathVariable Long dossierId,
            @RequestParam(required = false) String validateurNom) {
        return ResponseEntity.ok(fraisService.validerDossierParManager(dossierId, validateurNom));
    }

    @PreAuthorize("hasRole('MANAGER') or hasRole('RH') or hasRole('ADMIN')")
    @PutMapping("/dossiers/{dossierId}/refuser-manager")
    public ResponseEntity<DossierFraisDto> refuserDossierParManager(
            @PathVariable Long dossierId,
            @RequestParam(required = false) String validateurNom) {
        return ResponseEntity.ok(fraisService.refuserDossierParManager(dossierId, validateurNom));
    }

    @PreAuthorize("hasRole('RH') or hasRole('ADMIN')")
    @PutMapping("/demandes/{demandeId}/valider-rh")
    public ResponseEntity<DemandeFraisDto> validerParRh(
            @PathVariable Long demandeId,
            @RequestParam(required = false) String validateurNom) {
        return ResponseEntity.ok(fraisService.validerParRh(demandeId, validateurNom));
    }

    @PreAuthorize("hasRole('RH') or hasRole('ADMIN')")
    @PutMapping("/demandes/{demandeId}/refuser-rh")
    public ResponseEntity<DemandeFraisDto> refuserParRh(
            @PathVariable Long demandeId,
            @RequestParam(required = false) String validateurNom) {
        return ResponseEntity.ok(fraisService.refuserParRh(demandeId, validateurNom));
    }

    @PreAuthorize("hasRole('RH') or hasRole('ADMIN')")
    @PutMapping("/dossiers/{dossierId}/valider-rh")
    public ResponseEntity<DossierFraisDto> validerDossierParRh(
            @PathVariable Long dossierId,
            @RequestParam(required = false) String validateurNom) {
        return ResponseEntity.ok(fraisService.validerDossierParRh(dossierId, validateurNom));
    }

    @PreAuthorize("hasRole('RH') or hasRole('ADMIN')")
    @PutMapping("/dossiers/{dossierId}/refuser-rh")
    public ResponseEntity<DossierFraisDto> refuserDossierParRh(
            @PathVariable Long dossierId,
            @RequestParam(required = false) String validateurNom) {
        return ResponseEntity.ok(fraisService.refuserDossierParRh(dossierId, validateurNom));
    }

    @GetMapping(value = "/{employeeId}/historique/export-pdf", produces = "application/octet-stream")
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long employeeId) {
        List<DemandeFraisDto> list = fraisService.getHistorique(employeeId);
        byte[] content = buildExportContent(list);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=historique-frais.pdf")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(content);
    }

    private byte[] buildExportContent(List<DemandeFraisDto> list) {
        StringBuilder body = new StringBuilder();
        body.append("Historique des remboursements - Notes de frais\n\n");
        body.append("Date\tRef\tDescription\tCatégorie\tMontant (MAD)\tStatut\tRemboursé le\n");
        for (DemandeFraisDto d : list) {
            body.append(d.getDateSoumission()).append("\t")
                    .append(d.getReference()).append("\t")
                    .append(d.getDescription()).append("\t")
                    .append(d.getCategorieLabel()).append("\t")
                    .append(d.getMontant()).append("\t")
                    .append(d.getStatutLabel()).append("\t")
                    .append(d.getDateRemboursement()).append("\n");
        }
        return body.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}
