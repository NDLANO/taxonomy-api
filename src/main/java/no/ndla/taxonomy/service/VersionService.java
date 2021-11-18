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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Transactional(readOnly = true)
@Service
public class VersionService {
    private final VersionRepository versionRepository;

    public VersionService(VersionRepository versionRepository) {
        this.versionRepository = versionRepository;
    }

    @Transactional
    public void delete(URI publicId) {
        final var subjectToDelete = versionRepository.findFirstByPublicId(publicId)
                .orElseThrow(() -> new NotFoundServiceException("Subject was not found"));

        versionRepository.delete(subjectToDelete);
        versionRepository.flush();
    }

    public List<VersionDTO> getVersions() {
        return versionRepository.findAll().stream().map(VersionDTO::new).collect(Collectors.toList());
    }

    public List<VersionDTO> getVersionsOfType(VersionType versionType) {
        return versionRepository.findByVersionType(versionType).stream().map(VersionDTO::new)
                .collect(Collectors.toList());
    }

    public String getPublishedHash() {
        return getPublished().map(Version::getHash)
                .orElseThrow(() -> new NotFoundServiceException("No published version"));
    }

    public String getBetaVersionHash() {
        return getBeta().map(Version::getHash).orElseThrow(() -> new NotFoundServiceException("No beta version"));
    }

    public Optional<Version> getPublished() {
        return versionRepository.findFirstByVersionType(VersionType.PUBLISHED);
    }

    public Optional<Version> getBeta() {
        return versionRepository.findFirstByVersionType(VersionType.BETA);
    }

    public void publishBetaArchiveCurrentAddNew(URI id) {
        Optional<Version> published = getPublished();
        if (published.isPresent()) {
            Version version = published.get();
            version.setVersionType(VersionType.ARCHIVED);
            version.setArchived(Instant.now());
            versionRepository.save(version);
        }

        Version beta = versionRepository.getByPublicId(id);
        beta.setVersionType(VersionType.PUBLISHED);
        beta.setPublished(Instant.now());
        versionRepository.save(beta);

        Version newBeta = new Version();
        newBeta.setVersionType(VersionType.BETA);
        versionRepository.save(newBeta);
    }
}
