package com.hub.conges.service;

import com.hub.conges.entity.EmployeCongesInfo;
import com.hub.conges.entity.HistoriqueSolde;
import com.hub.conges.entity.SoldeConges;
import com.hub.conges.repository.EmployeCongesInfoRepository;
import com.hub.conges.repository.HistoriqueSoldeRepository;
import com.hub.conges.repository.SoldeCongesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AcquisitionCongesService {

    private final SoldeCongesRepository soldeRepository;
    private final HistoriqueSoldeRepository historiqueSoldeRepository;
    private final ParametresCongesService parametresCongesService;
    private final EmployeCongesInfoRepository employeCongesInfoRepository;

    @Transactional
    public int runAcquisitionMensuelle() {
        int annee = LocalDate.now().getYear();
        int mois = LocalDate.now().getMonthValue();
        BigDecimal valeur = parametresCongesService.getValeurAcquisitionMensuelle();
        double v = valeur.doubleValue();
        List<Long> employeeIds = soldeRepository.findDistinctEmployeeIdsByAnnee(annee);
        int count = 0;
        for (Long empId : employeeIds) {
            Optional<SoldeConges> opt = soldeRepository.findByEmployeeIdAndAnneeAndType(empId, annee, SoldeConges.TypeSolde.ANNUEL);
            if (opt.isEmpty()) continue;
            SoldeConges s = opt.get();
            s.setJoursTotal(s.getJoursTotal() + v);
            s.setDateDerniereMiseAJour(LocalDateTime.now());
            soldeRepository.save(s);
            historiqueSoldeRepository.save(HistoriqueSolde.builder()
                    .employeId(empId)
                    .typeMouvement(HistoriqueSolde.TypeMouvement.ACQUISITION_MENSUELLE)
                    .valeur(v)
                    .dateMouvement(LocalDateTime.now())
                    .annee(annee)
                    .libelle("Acquisition mensuelle " + mois + "/" + annee)
                    .build());
            count++;
        }
        log.info("Acquisition mensuelle: {} employés, +{} j/employé, {}/{}", count, valeur, mois, annee);
        return count;
    }

    @Transactional
    public SoldeConges initialiserSoldePourEmploye(Long employeeId, LocalDate dateEntree) {
        int annee = LocalDate.now().getYear();
        BigDecimal valeurMensuelle = parametresCongesService.getValeurAcquisitionMensuelle();
        int moisEntree = (dateEntree != null && dateEntree.getYear() == annee) ? dateEntree.getMonthValue() : 1;
        int moisTravailles = (dateEntree != null && dateEntree.getYear() == annee) ? (12 - moisEntree + 1) : 12;
        double total = valeurMensuelle.doubleValue() * moisTravailles;

        if (dateEntree != null && dateEntree.getYear() == annee) {
            employeCongesInfoRepository.findByEmployeId(employeeId).ifPresentOrElse(
                    info -> { info.setDateEntree(dateEntree); employeCongesInfoRepository.save(info); },
                    () -> employeCongesInfoRepository.save(EmployeCongesInfo.builder().employeId(employeeId).dateEntree(dateEntree).build())
            );
        }

        Optional<SoldeConges> existing = soldeRepository.findByEmployeeIdAndAnneeAndType(employeeId, annee, SoldeConges.TypeSolde.ANNUEL);
        if (existing.isPresent()) return existing.get();

        SoldeConges s = soldeRepository.save(SoldeConges.builder()
                .employeeId(employeeId)
                .type(SoldeConges.TypeSolde.ANNUEL)
                .annee(annee)
                .joursTotal(total)
                .joursPris(0)
                .dateDerniereMiseAJour(LocalDateTime.now())
                .build());
        HistoriqueSolde.TypeMouvement typeMov = (dateEntree != null && dateEntree.getYear() == annee)
                ? HistoriqueSolde.TypeMouvement.ACQUISITION_PRORATA : HistoriqueSolde.TypeMouvement.ACQUISITION_MENSUELLE;
        historiqueSoldeRepository.save(HistoriqueSolde.builder()
                .employeId(employeeId)
                .typeMouvement(typeMov)
                .valeur(total)
                .dateMouvement(LocalDateTime.now())
                .annee(annee)
                .libelle(typeMov == HistoriqueSolde.TypeMouvement.ACQUISITION_PRORATA ? "Acquisition prorata (" + moisTravailles + " mois)" : "Acquisition initiale")
                .build());
        return s;
    }
}
