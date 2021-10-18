/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.repositories.SubjectRepository;
import no.ndla.taxonomy.service.dtos.MetadataDto;
import no.ndla.taxonomy.service.dtos.SubjectDTO;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class SubjectServiceImpl implements SubjectService {
    private final SubjectRepository subjectRepository;
    private final MetadataApiService metadataApiService;
    private final EntityConnectionService entityConnectionService;

    public SubjectServiceImpl(SubjectRepository subjectRepository, MetadataApiService metadataApiService,
            EntityConnectionService entityConnectionService) {
        this.subjectRepository = subjectRepository;
        this.metadataApiService = metadataApiService;
        this.entityConnectionService = entityConnectionService;
    }

    @Override
    @Transactional
    public void delete(URI publicId) {
        final var subjectToDelete = subjectRepository.findFirstByPublicId(publicId)
                .orElseThrow(() -> new NotFoundServiceException("Subject was not found"));

        entityConnectionService.disconnectAllChildren(subjectToDelete);

        subjectRepository.delete(subjectToDelete);
        subjectRepository.flush();

        metadataApiService.deleteMetadataByPublicId(publicId);
    }

    @Override
    @MetadataQuery
    public List<SubjectDTO> getSubjects(String languageCode) {
        return subjectRepository.findAllIncludingCachedUrlsAndTranslations().stream()
                .map(subject -> new SubjectDTO(subject, languageCode)).collect(Collectors.toList());
    }

    @Override
    @MetadataQuery
    public List<SubjectDTO> getSubjects(String languageCode, MetadataKeyValueQuery metadataKeyValueQuery) {
        Set<String> publicIds = metadataKeyValueQuery.getDtos().stream().map(MetadataDto::getPublicId)
                .collect(Collectors.toSet());

        return publicIds.stream().map(subjectId -> {
            try {
                return new URI(subjectId);
            } catch (Exception e) {
                return null;
            }
        }).filter(Objects::nonNull).map(subjectRepository::findByPublicId).filter(Objects::nonNull)
                .map(subject -> new SubjectDTO(subject, languageCode)).collect(Collectors.toList());

    }
}
