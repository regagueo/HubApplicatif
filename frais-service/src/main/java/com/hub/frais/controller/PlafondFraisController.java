package com.hub.frais.controller;

import com.hub.frais.entity.PlafondFrais;
import com.hub.frais.service.PlafondFraisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/frais", "/frais"})
@RequiredArgsConstructor
public class PlafondFraisController {

    private final PlafondFraisService plafondFraisService;

    @GetMapping("/plafonds")
    public ResponseEntity<PlafondFrais> getPlafonds() {
        return ResponseEntity.ok(plafondFraisService.getPlafonds());
    }

    @PreAuthorize("hasRole('RH') or hasRole('ADMIN')")
    @PutMapping("/plafonds")
    public ResponseEntity<PlafondFrais> update(@RequestBody PlafondFrais payload,
                                               @RequestParam(required = false) String modifiePar) {
        return ResponseEntity.ok(plafondFraisService.update(payload, modifiePar));
    }
}
