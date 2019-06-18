package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.ResolvablePathEntity;
import no.ndla.taxonomy.domain.ResolvablePathEntityView;

import java.util.Optional;

public interface UpdateableEntityService {
    Optional<ResolvablePathEntity> getUpdateableEntity(ResolvablePathEntityView updateableEntityView);
}
