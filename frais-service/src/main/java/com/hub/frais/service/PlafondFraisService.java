package com.hub.frais.service;

import com.hub.frais.entity.PlafondFrais;
import com.hub.frais.repository.PlafondFraisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PlafondFraisService {

    private final PlafondFraisRepository plafondFraisRepository;

    public PlafondFrais getPlafonds() {
        return plafondFraisRepository.findAll().stream().findFirst().orElseGet(this::createDefaults);
    }

    @Transactional
    public PlafondFrais createDefaults() {
        return plafondFraisRepository.save(PlafondFrais.builder()
                .transportTarifKm(new BigDecimal("1.50"))
                .taxiForfaitSansJustificatif(new BigDecimal("50.00"))
                .repasMoins3Ans(new BigDecimal("80.00"))
                .repas3AnsEtPlus(new BigDecimal("120.00"))
                .repas3AnsEtPlusMax(new BigDecimal("150.00"))
                .hebergementCasablanca(new BigDecimal("800.00"))
                .hebergementAutresVilles(new BigDecimal("600.00"))
                .perDiemActif(Boolean.FALSE)
                .perDiemGlobal(new BigDecimal("0.00"))
                .dateMiseAJour(LocalDateTime.now())
                .modifiePar("SYSTEM")
                .build());
    }

    @Transactional
    public PlafondFrais update(PlafondFrais update, String modifiePar) {
        PlafondFrais p = getPlafonds();
        p.setTransportTarifKm(update.getTransportTarifKm());
        p.setTaxiForfaitSansJustificatif(update.getTaxiForfaitSansJustificatif());
        p.setRepasMoins3Ans(update.getRepasMoins3Ans());
        p.setRepas3AnsEtPlus(update.getRepas3AnsEtPlus());
        p.setRepas3AnsEtPlusMax(update.getRepas3AnsEtPlusMax());
        p.setHebergementCasablanca(update.getHebergementCasablanca());
        p.setHebergementAutresVilles(update.getHebergementAutresVilles());
        p.setPerDiemActif(update.getPerDiemActif());
        p.setPerDiemGlobal(update.getPerDiemGlobal());
        p.setDateMiseAJour(LocalDateTime.now());
        if (modifiePar != null && !modifiePar.isBlank()) {
            p.setModifiePar(modifiePar);
        }
        return plafondFraisRepository.save(p);
    }
}
