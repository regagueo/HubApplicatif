package com.hub.conges.service;

import com.hub.conges.entity.ParametresConges;
import com.hub.conges.repository.ParametresCongesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ParametresCongesService {

    private static final BigDecimal VALEUR_DEFAULT = new BigDecimal("1.50");

    private final ParametresCongesRepository parametresCongesRepository;

    public ParametresConges getParametres() {
        return parametresCongesRepository.findAll().stream().findFirst()
                .orElseGet(this::createDefault);
    }

    @Transactional
    public ParametresConges createDefault() {
        return parametresCongesRepository.save(ParametresConges.builder()
                .valeurAcquisitionMensuelle(VALEUR_DEFAULT)
                .dateMiseAJour(LocalDateTime.now())
                .creePar("SYSTEM")
                .build());
    }

    @Transactional
    public ParametresConges mettreAJour(BigDecimal valeurAcquisitionMensuelle, String creePar) {
        ParametresConges p = getParametres();
        p.setValeurAcquisitionMensuelle(valeurAcquisitionMensuelle);
        p.setDateMiseAJour(LocalDateTime.now());
        if (creePar != null) p.setCreePar(creePar);
        return parametresCongesRepository.save(p);
    }

    public BigDecimal getValeurAcquisitionMensuelle() {
        return getParametres().getValeurAcquisitionMensuelle();
    }
}
