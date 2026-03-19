package com.hub.employee.controller;

import com.hub.employee.dto.*;
import com.hub.employee.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/employee", "/api/employee"})
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping("/{id}/indicators")
    public ResponseEntity<IndicatorsDto> getIndicators(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getIndicators(id));
    }

    @GetMapping("/{id}/alerts")
    public ResponseEntity<List<AlerteDto>> getAlerts(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getAlerts(id));
    }

    @GetMapping("/{id}/conges")
    public ResponseEntity<List<DemandeCongesDto>> getConges(@PathVariable("id") Long employeeId) {
        return ResponseEntity.ok(employeeService.getConges(employeeId));
    }

    @PostMapping("/{id}/conges")
    public ResponseEntity<DemandeCongesDto> createDemandeConges(
            @PathVariable("id") Long employeeId,
            @Valid @RequestBody CreateDemandeCongesRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.createDemandeConges(employeeId, request));
    }

    @PutMapping("/{id}/conges/{congesId}")
    public ResponseEntity<DemandeCongesDto> updateDemandeConges(
            @PathVariable("id") Long employeeId,
            @PathVariable Long congesId,
            @Valid @RequestBody CreateDemandeCongesRequest request) {
        return ResponseEntity.ok(employeeService.updateDemandeConges(employeeId, congesId, request));
    }

    @DeleteMapping("/{id}/conges/{congesId}")
    public ResponseEntity<Void> deleteDemandeConges(
            @PathVariable("id") Long employeeId,
            @PathVariable Long congesId) {
        employeeService.deleteDemandeConges(employeeId, congesId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/notes-frais")
    public ResponseEntity<NotesFraisResponseDto> getNotesFrais(@PathVariable("id") Long employeeId) {
        return ResponseEntity.ok(employeeService.getNotesFrais(employeeId));
    }

    @GetMapping("/{id}/frais")
    public ResponseEntity<NotesFraisResponseDto> getFrais(@PathVariable("id") Long employeeId) {
        return ResponseEntity.ok(employeeService.getNotesFrais(employeeId));
    }

    @PostMapping("/{id}/frais")
    public ResponseEntity<NotesFraisResponseDto.RemboursementDto> createNoteFrais(
            @PathVariable("id") Long employeeId,
            @Valid @RequestBody CreateNoteFraisRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.createNoteFrais(employeeId, request));
    }

    @PutMapping("/{id}/frais/{fraisId}")
    public ResponseEntity<Void> updateNoteFrais(
            @PathVariable("id") Long employeeId,
            @PathVariable Long fraisId,
            @Valid @RequestBody CreateNoteFraisRequest request) {
        employeeService.updateNoteFrais(employeeId, fraisId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/frais/{fraisId}")
    public ResponseEntity<Void> deleteNoteFrais(
            @PathVariable("id") Long employeeId,
            @PathVariable Long fraisId) {
        employeeService.deleteNoteFrais(employeeId, fraisId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/colleagues")
    public ResponseEntity<List<CollaborateurDto>> getColleagues(@PathVariable("id") Long employeeId) {
        return ResponseEntity.ok(employeeService.getCollaborateurs(employeeId));
    }

    @GetMapping("/{id}/collaborateurs")
    public ResponseEntity<List<CollaborateurDto>> getCollaborateurs(@PathVariable("id") Long employeeId) {
        return ResponseEntity.ok(employeeService.getCollaborateurs(employeeId));
    }
}
