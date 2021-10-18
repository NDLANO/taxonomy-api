/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.service.dtos.*;

import java.net.URI;
import java.util.List;
import java.util.Set;

public interface ResourceService {
    void delete(URI id);

    List<ResourceWithTopicConnectionDTO> getResourcesByTopicId(URI topicPublicId, URI filterBySubjectId,
            Set<URI> resourceTypeIds, URI relevancePublicId, String languageCode, boolean recursive);

    List<ResourceWithTopicConnectionDTO> getResourcesBySubjectId(URI subjectPublicId, Set<URI> resourceTypeIds,
            URI relevancePublicId, String languageCode);

    List<ResourceWithNodeConnectionDTO> getResourcesByNodeId(URI nodePublicId, Set<URI> resourceTypeIds,
            URI relevancePublicId, String languageCode, boolean recursive);

    ResourceDTO getResourceByPublicId(URI publicId, String languageCode);

    ResourceWithParentTopicsDTO getResourceWithParentTopicsByPublicId(URI publicId, String languageCode);

    ResourceWithParentNodesDTO getResourceWithParentNodesByPublicId(URI publicId, String languageCode);

    List<ResourceDTO> getResources(String languageCode, URI contentUriFilter);

    List<ResourceDTO> getResources(String languageCode, URI contentUriFilter,
            MetadataKeyValueQuery metadataKeyValueQuery);
}
