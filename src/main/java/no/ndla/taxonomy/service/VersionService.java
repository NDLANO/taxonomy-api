/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.VersionType;
import no.ndla.taxonomy.repositories.VersionRepository;
import no.ndla.taxonomy.service.dtos.SubjectDTO;
import no.ndla.taxonomy.service.dtos.VersionDTO;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.List;
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
}
