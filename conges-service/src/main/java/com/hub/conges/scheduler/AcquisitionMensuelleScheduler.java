package com.hub.conges.scheduler;

import com.hub.conges.service.AcquisitionCongesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AcquisitionMensuelleScheduler {

    private final AcquisitionCongesService acquisitionCongesService;

    @Scheduled(cron = "${app.conges.acquisition.cron:0 0 1 * * ?}")
    public void runAcquisitionMensuelle() {
        log.info("Démarrage acquisition mensuelle des congés");
        try {
            int count = acquisitionCongesService.runAcquisitionMensuelle();
            log.info("Acquisition mensuelle terminée: {} employés", count);
        } catch (Exception e) {
            log.error("Erreur acquisition mensuelle", e);
        }
    }
}
