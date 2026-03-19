package com.hub.conges.service;

import com.hub.conges.client.NagerDateHolidayDto;
import com.hub.conges.entity.JoursFeries;
import com.hub.conges.repository.JoursFeriesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JoursFeriesService {

    private static final String NAGER_API_URL = "https://date.nager.at/api/v3/PublicHolidays";

    private final JoursFeriesRepository joursFeriesRepository;
    private final RestTemplate restTemplate;

    @Value("${app.nager.country:MA}")
    private String countryCode;

    public Set<LocalDate> getJoursFeriesPourAnnee(int annee) {
        List<JoursFeries> cached = joursFeriesRepository.findByAnneeOrderByDateAsc(annee);
        if (!cached.isEmpty()) {
            return cached.stream().map(JoursFeries::getDate).collect(Collectors.toSet());
        }
        syncAnneeFromNager(annee);
        return joursFeriesRepository.findByAnneeOrderByDateAsc(annee).stream()
                .map(JoursFeries::getDate).collect(Collectors.toSet());
    }

    @Transactional
    public int syncAnneeFromNager(int annee) {
        String url = NAGER_API_URL + "/" + annee + "/" + countryCode;
        try {
            ParameterizedTypeReference<List<NagerDateHolidayDto>> typeRef = new ParameterizedTypeReference<>() {};
            List<NagerDateHolidayDto> list = restTemplate.exchange(url, HttpMethod.GET, null, typeRef).getBody();
            if (list == null) return 0;
            int added = 0;
            for (NagerDateHolidayDto dto : list) {
                if (dto.getDate() == null) continue;
                if (joursFeriesRepository.existsByDateAndAnnee(dto.getDate(), annee)) continue;
                JoursFeries jf = JoursFeries.builder()
                        .date(dto.getDate())
                        .libelle(dto.getLocalName() != null ? dto.getLocalName() : dto.getName())
                        .annee(annee)
                        .source(JoursFeries.SourceJoursFeries.NAGER_API)
                        .build();
                joursFeriesRepository.save(jf);
                added++;
            }
            log.info("Jours fériés Nager: {} entrées pour année {}", added, annee);
            return added;
        } catch (Exception e) {
            log.warn("Erreur sync Nager pour année {}: {}", annee, e.getMessage());
            return 0;
        }
    }

    public List<JoursFeries> listByAnnee(int annee) {
        return joursFeriesRepository.findByAnneeOrderByDateAsc(annee);
    }

    @Transactional
    public JoursFeries ajouterManuel(LocalDate date, String libelle) {
        int annee = date.getYear();
        if (joursFeriesRepository.existsByDateAndAnnee(date, annee)) {
            throw new IllegalArgumentException("Un jour férié existe déjà pour cette date");
        }
        return joursFeriesRepository.save(JoursFeries.builder()
                .date(date)
                .libelle(libelle != null ? libelle : "Jour férié exceptionnel")
                .annee(annee)
                .source(JoursFeries.SourceJoursFeries.MANUEL)
                .build());
    }

    @Transactional
    public void supprimer(Long id) {
        joursFeriesRepository.deleteById(id);
    }
}
