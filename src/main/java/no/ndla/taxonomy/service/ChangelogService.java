/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Changelog;
import no.ndla.taxonomy.domain.DomainEntity;
import no.ndla.taxonomy.repositories.ChangelogRepository;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import no.ndla.taxonomy.service.task.Fetcher;
import no.ndla.taxonomy.service.task.Updater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ConcurrentModificationException;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class ChangelogService implements DisposableBean {
    final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ChangelogRepository changelogRepository;
    private final DomainEntityHelperService domainEntityHelperService;
    private final VersionService versionService;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public ChangelogService(ChangelogRepository changelogRepository,
            DomainEntityHelperService domainEntityHelperService, VersionService versionService) {
        this.changelogRepository = changelogRepository;
        this.domainEntityHelperService = domainEntityHelperService;
        this.versionService = versionService;
    }

    @Transactional
    @Scheduled(fixedRate = 10, timeUnit = TimeUnit.SECONDS)
    public void removeFinishedChangelogs() {
        changelogRepository.deleteByDoneTrue();
    }

    @Scheduled(fixedRate = 2, timeUnit = TimeUnit.SECONDS)
    @Transactional
    public void processChanges() {
        try {
            Optional<Changelog> maybeChangelog = changelogRepository.findFirstByDoneFalse();
            if (maybeChangelog.isPresent()) {
                Changelog changelog = maybeChangelog.get();
                if (changelog.isDone()) {
                    return;
                }
                DomainEntity entity;
                // Fetch
                try {
                    Fetcher fetcher = new Fetcher();
                    fetcher.setVersion(versionService.schemaFromHash(changelog.getSourceSchema()));
                    fetcher.setPublicId(changelog.getPublicId());
                    fetcher.setCleanUp(changelog.isCleanUp());
                    fetcher.setDomainEntityHelperService(domainEntityHelperService);

                    Future<DomainEntity> future = executor.submit(fetcher);
                    entity = future.get();
                } catch (Exception e) {
                    logger.info(e.getMessage(), e);
                    throw new NotFoundServiceException("Failed to fetch node from source schema", e);
                }
                // Update
                try {
                    Updater updater = new Updater();
                    updater.setVersion(versionService.schemaFromHash(changelog.getDestinationSchema()));
                    updater.setElement(entity);
                    updater.setCleanUp(changelog.isCleanUp());
                    updater.setDomainEntityHelperService(domainEntityHelperService);

                    Future<DomainEntity> future = executor.submit(updater);
                    future.get();
                } catch (Exception e) {
                    logger.info(e.getMessage(), e);
                    throw new NotFoundServiceException("Failed to update entity", e);
                }
                changelog.setDone(true);
                changelogRepository.save(changelog);
            }
        } catch (ConcurrentModificationException cme) {
            try {
                logger.info("Another server tried to change entity at the same time: " + cme.getMessage());
                var random = new Random();
                Thread.sleep(1000 + random.nextInt(5000));
            } catch (InterruptedException ie) {
                // Nothing to see here
            }
        } catch (Exception exception) {
            logger.info("Failed to process entity " + exception.getMessage());
        }
    }

    @Override
    public void destroy() throws Exception {
        executor.shutdown();
    }
}
