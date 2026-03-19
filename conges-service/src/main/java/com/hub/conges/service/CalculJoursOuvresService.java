package com.hub.conges.service;

import com.hub.conges.entity.DemandeConges;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CalculJoursOuvresService {

    private final JoursFeriesService joursFeriesService;

    public static class ResultatCalcul {
        private final double joursOuvres;
        private final int weekEndsExclus;
        private final int joursFeriesExclus;

        public ResultatCalcul(double joursOuvres, int weekEndsExclus, int joursFeriesExclus) {
            this.joursOuvres = joursOuvres;
            this.weekEndsExclus = weekEndsExclus;
            this.joursFeriesExclus = joursFeriesExclus;
        }

        public double getJoursOuvres() { return joursOuvres; }
        public int getWeekEndsExclus() { return weekEndsExclus; }
        public int getJoursFeriesExclus() { return joursFeriesExclus; }
    }

    public ResultatCalcul calculer(LocalDate dateDebut, LocalDate dateFin, boolean demiJournee) {
        if (dateDebut.isAfter(dateFin)) {
            return new ResultatCalcul(0, 0, 0);
        }
        int anneeDebut = dateDebut.getYear();
        int anneeFin = dateFin.getYear();
        Set<LocalDate> feries = new HashSet<>();
        for (int annee = anneeDebut; annee <= anneeFin; annee++) {
            feries.addAll(joursFeriesService.getJoursFeriesPourAnnee(annee));
        }
        int weekEnds = 0;
        int feriesExclus = 0;
        double jours = 0;
        LocalDate cur = dateDebut;
        while (!cur.isAfter(dateFin)) {
            DayOfWeek dow = cur.getDayOfWeek();
            boolean isWeekEnd = dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
            boolean isFerie = feries.contains(cur);
            if (isWeekEnd) weekEnds++;
            if (isFerie) feriesExclus++;
            if (!isWeekEnd && !isFerie) {
                jours += demiJournee ? 0.5 : 1.0;
            }
            cur = cur.plusDays(1);
        }
        return new ResultatCalcul(jours, weekEnds, feriesExclus);
    }

    public double calculerJoursOuvres(LocalDate dateDebut, LocalDate dateFin, DemandeConges.PeriodeType periode) {
        return calculer(dateDebut, dateFin, periode == DemandeConges.PeriodeType.DEMI_JOURNEE).getJoursOuvres();
    }
}
