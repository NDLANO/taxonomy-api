package no.ndla.taxonomy.service;

import no.ndla.taxonomy.repositories.CachedPathRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
public class CachedPathCleaner {
    private final CachedPathRepository cachedPathRepository;
    private final int rate = 5 * 60 * 1000; // Every five minutes

    public CachedPathCleaner(CachedPathRepository cachedPathRepository) {
        this.cachedPathRepository = cachedPathRepository;
    }

    @Scheduled(fixedRate = rate)
    @Transactional
    public void removeInactivePaths() {
        cachedPathRepository.deleteByActiveFalse();
    }
}
