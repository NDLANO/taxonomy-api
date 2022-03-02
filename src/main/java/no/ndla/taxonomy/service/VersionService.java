/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Version;
import no.ndla.taxonomy.domain.VersionType;
import no.ndla.taxonomy.repositories.VersionRepository;
import no.ndla.taxonomy.service.dtos.VersionDTO;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Transactional(readOnly = true)
@Service
public class VersionService {
    private final VersionRepository versionRepository;
    private EntityManager entityManager;

    @Value("${spring.datasource.hikari.schema:public}") // Default value used in test.
    private String defaultSchema;

    public VersionService(VersionRepository versionRepository, EntityManager entityManager) {
        this.versionRepository = versionRepository;
        this.entityManager = entityManager;
    }

    @Transactional
    public void delete(URI publicId) {
        final var versionToDelete = versionRepository.findFirstByPublicId(publicId)
                .orElseThrow(() -> new NotFoundServiceException("Version was not found"));

        versionRepository.delete(versionToDelete);
        versionRepository.flush();
    }

    public List<VersionDTO> getVersions() {
        return versionRepository.findAll().stream().map(VersionDTO::new).collect(Collectors.toList());
    }

    public List<VersionDTO> getVersionsOfType(VersionType versionType) {
        return versionRepository.findByVersionType(versionType).stream().map(VersionDTO::new)
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
        beta.setPublished(Instant.now());

        String schema = String.format("%s_%s", defaultSchema, beta.getHash());
        entityManager.createNativeQuery(String.format("SELECT clone_schema('%s', '%s', true, false)", defaultSchema, schema)).executeUpdate();
        versionRepository.save(beta);
    }
}
