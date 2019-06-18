package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.ResolvablePathEntity;

import java.util.List;

public interface PathUpdaterService {
    List<ResolvablePathEntity> getEntitiesToUpdate();

    void updateEntity(ResolvablePathEntity updateableEntity);
}
