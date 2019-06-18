package no.ndla.taxonomy.task;

import no.ndla.taxonomy.service.PathUpdaterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class GeneratedPathUpdaterTask {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private PathUpdaterService pathUpdaterService;

    GeneratedPathUpdaterTask(PathUpdaterService pathUpdaterService) {
        this.pathUpdaterService = pathUpdaterService;
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional(readOnly = true)
    public void run() {
        log.info("Running GeneratedPathUpdaterTask:run");

        // TODO: Add locking
        pathUpdaterService.getEntitiesToUpdate().forEach(entityToUpdate -> {
            log.info("Updating entity " + entityToUpdate.getType() + ":" + entityToUpdate.getId());
            pathUpdaterService.updateEntity(entityToUpdate);
        });

    }
}
