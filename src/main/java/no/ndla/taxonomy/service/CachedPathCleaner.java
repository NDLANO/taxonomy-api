/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.repositories.CachedPathRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.concurrent.TimeUnit;

@Service
public class CachedPathCleaner {
    private final CachedPathRepository cachedPathRepository;

    public CachedPathCleaner(CachedPathRepository cachedPathRepository) {
        this.cachedPathRepository = cachedPathRepository;
    }

    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.MINUTES)
    @Transactional
    public void removeInactivePaths() {
        cachedPathRepository.deleteByActiveFalse();
    }
}
