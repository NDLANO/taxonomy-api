package no.ndla.taxonomy.service;

import no.ndla.taxonomy.repositories.CachedPathRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
public class CachedPathCleaner {
    private final CachedPathRepository cachedPathRepository;

    public CachedPathCleaner(CachedPathRepository cachedPathRepository) {
        this.cachedPathRepository = cachedPathRepository;
    }

    @Scheduled(fixedRate = 10000)
    @Transactional
    public void removeInactivePaths() {
        cachedPathRepository.deleteByActiveFalse();
    }
}
