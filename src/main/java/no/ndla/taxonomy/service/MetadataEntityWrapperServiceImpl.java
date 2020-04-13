package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.DomainEntity;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MetadataEntityWrapperServiceImpl implements MetadataEntityWrapperService {
    private final MetadataApiService metadataApiService;

    public MetadataEntityWrapperServiceImpl(MetadataApiService metadataApiService) {
        this.metadataApiService = metadataApiService;
    }

    @Override
    public <T extends DomainEntity> List<MetadataWrappedEntity<T>> wrapEntities(List<T> entities, boolean readMetadata, Function<T, URI> publicIdProvider) {
        if (!readMetadata) {
            return entities.stream()
                    .map(MetadataWrappedEntity::new)
                    .collect(Collectors.toList());
        }

        // First reads all metadata objects from service, then creates a wrapped object of each entity with its
        // corresponding metadata object from the list retrieved

        final var metadataObjects = metadataApiService.getMetadataByPublicId(entities.stream().map(publicIdProvider).collect(Collectors.toSet()));

        final var listToReturn = new ArrayList<MetadataWrappedEntity<T>>();

        entities.forEach(entity -> {
            metadataObjects.stream()
                    .filter(metadataDto -> metadataDto.getPublicId().equals(publicIdProvider.apply(entity).toString()))
                    .findFirst()
                    .ifPresentOrElse(
                            metadata -> listToReturn.add(new MetadataWrappedEntity<>(entity, metadata)),
                            () -> listToReturn.add(new MetadataWrappedEntity<>(entity))
                    );
        });

        return listToReturn;
    }

    @Override
    public <T extends DomainEntity> List<MetadataWrappedEntity<T>> wrapEntities(List<T> entities, boolean readMetadata) {
        return this.wrapEntities(entities, readMetadata, DomainEntity::getPublicId);
    }

    @Override
    public <T extends DomainEntity> MetadataWrappedEntity<T> wrapEntity(T entity, boolean readMetadata) {
        if (!readMetadata) {
            return new MetadataWrappedEntity<>(entity);
        }

        return new MetadataWrappedEntity<>(entity, metadataApiService.getMetadataByPublicId(entity.getPublicId()));
    }
}
