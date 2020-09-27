package no.ndla.taxonomy.service;

import no.ndla.taxonomy.service.dtos.ResourceDTO;
import no.ndla.taxonomy.service.dtos.ResourceWithParentTopicsDTO;
import no.ndla.taxonomy.service.dtos.ResourceWithTopicConnectionDTO;

import java.net.URI;
import java.util.List;
import java.util.Set;

public interface ResourceService {
    void delete(URI id);

    List<ResourceWithTopicConnectionDTO> getResourcesByTopicId(URI topicPublicId, Set<URI> filterIds,
                                                               URI filterBySubjectId,
                                                               Set<URI> resourceTypeIds,
                                                               URI relevancePublicId,
                                                               String languageCode, boolean recursive);

    List<ResourceWithTopicConnectionDTO> getResourcesBySubjectId(URI subjectPublicId, Set<URI> filterIds,
                                                                 Set<URI> resourceTypeIds, URI relevancePublicId,
                                                                 String languageCode);

    ResourceDTO getResourceByPublicId(URI publicId, String languageCode);

    ResourceWithParentTopicsDTO getResourceWithParentTopicsByPublicId(URI publicId, String languageCode);

    List<ResourceDTO> getResources(String languageCode, URI contentUriFilter);
}
