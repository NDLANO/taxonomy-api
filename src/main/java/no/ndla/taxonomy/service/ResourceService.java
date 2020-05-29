package no.ndla.taxonomy.service;

import no.ndla.taxonomy.service.dtos.ResourceDTO;
import no.ndla.taxonomy.service.dtos.ResourceWithParentTopicsDTO;
import no.ndla.taxonomy.service.dtos.ResourceWithTopicConnectionDTO;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;
import java.util.Set;

public interface ResourceService {
    void delete(URI id);

    List<ResourceWithTopicConnectionDTO> getResourcesByTopicId(@NotNull URI topicPublicId, @NotNull Set<URI> filterIds,
                                                               URI filterBySubjectId,
                                                               @NotNull Set<URI> resourceTypeIds,
                                                               URI relevancePublicId,
                                                               String languageCode, boolean recursive, boolean includeMetadata);

    List<ResourceWithTopicConnectionDTO> getResourcesBySubjectId(@NotNull URI subjectPublicId, @NotNull Set<URI> filterIds,
                                                                 @NotNull Set<URI> resourceTypeIds, URI relevancePublicId,
                                                                 String languageCode, boolean includeMetadata);

    ResourceDTO getResourceByPublicId(@NotNull URI publicId, String languageCode, boolean includeMetadata);

    ResourceWithParentTopicsDTO getResourceWithParentTopicsByPublicId(@NotNull URI publicId, String languageCode);

    List<ResourceDTO> getResources(String languageCode, boolean includeMetadata);
}
