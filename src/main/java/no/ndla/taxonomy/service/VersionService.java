/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import no.ndla.taxonomy.domain.DomainEntity;
import no.ndla.taxonomy.domain.Version;
import no.ndla.taxonomy.domain.VersionType;
import no.ndla.taxonomy.repositories.NodeConnectionRepository;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.repositories.VersionRepository;
import no.ndla.taxonomy.rest.v1.commands.VersionPostPut;
import no.ndla.taxonomy.service.dtos.VersionDTO;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import no.ndla.taxonomy.service.task.Deleter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@Service
public class VersionService {
    final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final EntityManager entityManager;
    private final VersionRepository versionRepository;
    private final NodeRepository nodeRepository;
    private final NodeConnectionRepository nodeConnectionRepository;
    private final NodeService nodeService;
    private final URNValidator validator = new URNValidator();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Value("${spring.datasource.hikari.schema:taxonomy_api}")
    private String defaultSchema;

    public VersionService(
            EntityManager entityManager,
            VersionRepository versionRepository,
            NodeRepository nodeRepository,
            NodeConnectionRepository nodeConnectionRepository,
            NodeService nodeService) {
        this.entityManager = entityManager;
        this.versionRepository = versionRepository;
        this.nodeRepository = nodeRepository;
        this.nodeConnectionRepository = nodeConnectionRepository;
        this.nodeService = nodeService;
    }

    @Transactional
    public void delete(URI publicId) {
        final var versionToDelete = versionRepository
                .findFirstByPublicId(publicId)
                .orElseThrow(() -> new NotFoundServiceException("Version was not found"));

        String schema = schemaFromHash(versionToDelete.getHash());
        try {
            entityManager
                    .createNativeQuery(String.format("DROP SCHEMA %s CASCADE", schema))
                    .executeUpdate();
        } catch (PersistenceException pe) {
            logger.warn("Failed to drop schema. Possible manual cleanup required");
        }
        versionRepository.delete(versionToDelete);
    }

    public List<VersionDTO> getVersions() {
        return versionRepository.findAll().stream().map(VersionDTO::new).collect(Collectors.toList());
    }

    public List<VersionDTO> getVersionsOfType(VersionType versionType) {
        return versionRepository.findByVersionType(versionType).stream()
                .map(VersionDTO::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public void publishBetaAndArchiveCurrent(URI id) {
        Optional<Version> published = versionRepository.findFirstByVersionType(VersionType.PUBLISHED);
        if (published.isPresent()) {
            Version version = published.get();
            version.setVersionType(VersionType.ARCHIVED);
            version.setArchived(Instant.now());
            versionRepository.save(version);
        }
        Version beta = versionRepository.getByPublicId(id);
        beta.setVersionType(VersionType.PUBLISHED);
        beta.setLocked(true);
        beta.setPublished(Instant.now());
        versionRepository.save(beta);

        disconnectAllInvisibleNodes(beta.getHash());
    }

    private void disconnectAllInvisibleNodes(String hash) {
        // Use a task to run in a separate thread against a specified schema
        try {
            Deleter deleter = new Deleter();
            deleter.setNodeService(nodeService);
            deleter.setVersion(schemaFromHash(hash));
            Future<DomainEntity> future = executor.submit(deleter);
            future.get();
        } catch (Exception e) {
            logger.info(e.getMessage(), e);
            // throw new NotFoundServiceException("Failed to disconnect invisible children in schema", e);
        }
    }

    @Transactional
    public Version createNewVersion(Optional<URI> sourceId, VersionPostPut command) {
        Version entity = new Version();
        command.getId().ifPresent(id -> {
            validator.validate(id, entity);
            entity.setPublicId(id);
        });
        command.apply(entity);
        Version version = versionRepository.save(entity);

        String sourceSchema = defaultSchema;
        if (sourceId.isPresent()) {
            Version source = versionRepository.getByPublicId(sourceId.get());
            if (source != null) {
                sourceSchema = schemaFromHash(source.getHash());
            }
        }

        String schema = schemaFromHash(version.getHash());
        // JPA does not like functions returning void so adds a count(*) to sql.
        entityManager
                .createNativeQuery(String.format(
                        "SELECT count(*) from clone_schema('%s', '%s', true, false)", sourceSchema, schema))
                .getSingleResult();
        return version;
    }

    public String schemaFromHash(String hash) {
        if (hash != null) return String.format("%s%s", defaultSchema, "_" + hash);
        return defaultSchema;
    }
}
