package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.DomainEntity;

import java.net.URI;
import java.util.List;
import java.util.function.Function;

public interface MetadataEntityWrapperService {
    <T extends DomainEntity> List<MetadataWrappedEntity<T>> wrapEntities(List<T> entities, boolean readMetadata, Function<T, URI> publicIdProvider);

    <T extends DomainEntity> List<MetadataWrappedEntity<T>> wrapEntities(List<T> entities, boolean readMetadata);

    <T extends DomainEntity> MetadataWrappedEntity<T> wrapEntity(T entity, boolean readMetadata);
}
