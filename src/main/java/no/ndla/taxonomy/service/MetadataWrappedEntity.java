package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.DomainEntity;
import no.ndla.taxonomy.service.dtos.MetadataDto;

import java.util.Optional;

public class MetadataWrappedEntity<T extends DomainEntity> {
    private final T entity;
    private final MetadataDto metadata;

    public MetadataWrappedEntity(T entity, MetadataDto metadata) {
        this.entity = entity;
        this.metadata = metadata;
    }

    public MetadataWrappedEntity(T entity) {
        this.entity = entity;
        this.metadata = null;
    }

    public T getEntity() {
        return entity;
    }

    public Optional<MetadataDto> getMetadata() {
        return Optional.ofNullable(metadata);
    }
}