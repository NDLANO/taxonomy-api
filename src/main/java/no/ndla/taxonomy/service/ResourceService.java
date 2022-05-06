/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Resource;
import no.ndla.taxonomy.repositories.ResourceRepository;
import no.ndla.taxonomy.service.dtos.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ResourceService extends SearchService<ResourceDTO, Resource, ResourceRepository> {
    void delete(URI id);

    List<ResourceWithNodeConnectionDTO> getResourcesByNodeId(URI nodePublicId, Set<URI> resourceTypeIds,
            URI relevancePublicId, String languageCode, boolean recursive);

    ResourceDTO getResourceByPublicId(URI publicId, String languageCode);

    ResourceWithParentsDTO getResourceWithParentNodesByPublicId(URI publicId, String languageCode);

    List<ResourceDTO> getResources(Optional<String> language, Optional<URI> contentUri,
            MetadataFilters metadataFilters);

    Resource publishResource(URI resourceId, Optional<URI> sourceId, URI targetId);

    Resource cloneResource(URI publicId, URI contentUri);
}
