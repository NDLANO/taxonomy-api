package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.EntityWithPath;
import no.ndla.taxonomy.domain.Resource;
import no.ndla.taxonomy.repositories.ResourceRepository;
import no.ndla.taxonomy.repositories.TopicRepository;
import no.ndla.taxonomy.service.dtos.MetadataDto;
import no.ndla.taxonomy.service.dtos.RecursiveMergeResultDto;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class MetadataUpdateServiceImpl implements MetadataUpdateService {
    private final TopicRepository topicRepository;
    private final ResourceRepository resourceRepository;

    private final MetadataApiService apiService;

    public MetadataUpdateServiceImpl(TopicRepository topicRepository, ResourceRepository resourceRepository, MetadataApiService apiService) {
        this.topicRepository = topicRepository;
        this.resourceRepository = resourceRepository;
        this.apiService = apiService;
    }

    private EntityWithPath getEntityFromPublicId(URI publicId) {
        switch (publicId.getSchemeSpecificPart().split(":")[0]) {
            case "subject":
            case "topic":
                return topicRepository.findFirstByPublicId(publicId).orElseThrow(() -> new NotFoundServiceException("Subject/Topic by id was not found"));
            case "resource":
                return resourceRepository.findFirstByPublicId(publicId).orElseThrow(() -> new NotFoundServiceException("Resource by id was not found"));
        }

        throw new NotFoundServiceException("Unknown entity requested");
    }

    private Set<URI> getEntityPublicIdsToUpdateRecursively(EntityWithPath entity, boolean applyToResources) {
        final var entitiesToUpdate = new HashSet<>(Set.of(entity.getPublicId()));

        // Retrieves all publicIds recursively for the requested entity, ignoring Resources if applyToResources = false

        entity.getChildConnections().stream()
                .map(connection -> connection.getConnectedChild().orElse(null))
                .filter(Objects::nonNull)
                .filter(e -> applyToResources || !(e instanceof Resource))
                .flatMap(entityToUpdate -> getEntityPublicIdsToUpdateRecursively(entityToUpdate, applyToResources).stream())
                .forEach(entitiesToUpdate::add);

        return entitiesToUpdate;
    }

    @Override
    public RecursiveMergeResultDto updateMetadataRecursivelyByPublicId(URI publicId, MetadataDto metadataApiEntity, boolean applyToResources) {
        return new RecursiveMergeResultDto(apiService.updateMetadataByPublicIds(getEntityPublicIdsToUpdateRecursively(getEntityFromPublicId(publicId), applyToResources), metadataApiEntity));
    }

    @Override
    public MetadataApiService getMetadataApiService() {
        return apiService;
    }
}
