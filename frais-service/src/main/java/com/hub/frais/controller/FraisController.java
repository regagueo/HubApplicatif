package com.hub.frais.controller;

import com.hub.frais.dto.DemandeFraisDto;
import com.hub.frais.dto.EncoursFraisDto;
import com.hub.frais.service.FraisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
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
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "justificatif", required = false) MultipartFile justificatif) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fraisService.createDemande(employeeId, montant, categorie, description, justificatif));
    }

    @GetMapping("/{employeeId}/encours")
    public ResponseEntity<EncoursFraisDto> getEncours(@PathVariable Long employeeId) {
        return ResponseEntity.ok(fraisService.getEncours(employeeId));
    }

    @GetMapping("/{employeeId}/historique")
    public ResponseEntity<List<DemandeFraisDto>> getHistorique(@PathVariable Long employeeId) {
        return ResponseEntity.ok(fraisService.getHistorique(employeeId));
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
