package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Filter;
import no.ndla.taxonomy.domain.Resource;
import no.ndla.taxonomy.domain.TopicResource;
import no.ndla.taxonomy.repositories.ResourceRepository;
import no.ndla.taxonomy.repositories.TopicResourceRepository;
import no.ndla.taxonomy.rest.NotFoundHttpResponseException;
import no.ndla.taxonomy.service.dtos.ResourceDTO;
import no.ndla.taxonomy.service.dtos.ResourceWithParentTopicsDTO;
import no.ndla.taxonomy.service.dtos.ResourceWithTopicConnectionDTO;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ResourceServiceImpl implements ResourceService {
    private final ResourceRepository resourceRepository;
    private final EntityConnectionService connectionService;
    private final MetadataApiService metadataApiService;
    private final DomainEntityHelperService domainEntityHelperService;
    private final RecursiveTopicTreeService recursiveTopicTreeService;
    private final TopicResourceRepository topicResourceRepository;
    private final TopicTreeSorter topicTreeSorter;
    private final MetadataEntityWrapperService metadataEntityWrapperService;

    public ResourceServiceImpl(ResourceRepository resourceRepository, TopicResourceRepository topicResourceRepository,
                               EntityConnectionService connectionService, MetadataApiService metadataApiService,
                               DomainEntityHelperService domainEntityHelperService, RecursiveTopicTreeService recursiveTopicTreeService,
                               TopicTreeSorter topicTreeSorter, MetadataEntityWrapperService metadataEntityWrapperService) {
        this.resourceRepository = resourceRepository;
        this.connectionService = connectionService;
        this.metadataApiService = metadataApiService;
        this.domainEntityHelperService = domainEntityHelperService;
        this.recursiveTopicTreeService = recursiveTopicTreeService;
        this.topicResourceRepository = topicResourceRepository;
        this.topicTreeSorter = topicTreeSorter;
        this.metadataEntityWrapperService = metadataEntityWrapperService;
    }

    @Override
    @Transactional
    public void delete(URI id) {
        final var resourceToDelete = resourceRepository.findFirstByPublicId(id).orElseThrow(() -> new NotFoundServiceException("Subject was not found"));

        // ATM resources can not have any children, but still implements the interface that could have children
        connectionService.disconnectAllChildren(resourceToDelete);

        resourceRepository.delete(resourceToDelete);
        resourceRepository.flush();

        metadataApiService.deleteMetadataByPublicId(id);
    }

    private List<ResourceWithTopicConnectionDTO> filterTopicResourcesByIdsAndReturn(Set<Integer> topicIds, Set<URI> filterIds, Set<URI> resourceTypeIds, URI relevance,
                                                                                    Set<TopicResourceTreeSortable> sortableListToAddTo,
                                                                                    String languageCode, boolean includeMetadata) {
        final List<TopicResource> topicResources;

        if (filterIds.size() > 0 && resourceTypeIds.size() > 0) {
            topicResources = topicResourceRepository.findAllByTopicIdsAndResourceFilterFilterPublicIdsAndResourceTypePublicIdsAndRelevancePublicIdIfNotNullIncludingRelationsForResourceDocuments(topicIds, filterIds, resourceTypeIds, relevance);
        } else if (filterIds.size() > 0) {
            topicResources = topicResourceRepository.findAllByTopicIdsAndResourceFilterFilterPublicIdsAndRelevancePublicIdIfNotNullIncludingRelationsForResourceDocuments(topicIds, filterIds, relevance);
        } else if (resourceTypeIds.size() > 0) {
            topicResources = topicResourceRepository.findAllByTopicIdsAndResourceTypePublicIdsAndRelevancePublicIdIfNotNullIncludingRelationsForResourceDocuments(topicIds, resourceTypeIds, relevance);
        } else {
            topicResources = topicResourceRepository.findAllByTopicIdsAndRelevancePublicIdIfNotNullIncludingRelationsForResourceDocuments(topicIds, relevance);
        }

        topicResources.forEach(topicResource -> sortableListToAddTo.add(new TopicResourceTreeSortable(topicResource)));

        // Sort the list, extract all the topicResource objects in between topics and return list of documents

        final var sortedList = topicTreeSorter
                .sortList(sortableListToAddTo)
                .stream()
                .map(TopicResourceTreeSortable::getTopicResource)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        return metadataEntityWrapperService.wrapEntities(sortedList, includeMetadata, (entity) -> entity.getResource().map(Resource::getPublicId).orElse(null)).stream()
                .map(wrappedTopicResource -> new ResourceWithTopicConnectionDTO(wrappedTopicResource, languageCode))
                .collect(Collectors.toList());

    }

    @Override
    public List<ResourceWithTopicConnectionDTO> getResourcesBySubjectId(@NotNull URI subjectPublicId, @NotNull Set<URI> filterIds,
                                                                        @NotNull Set<URI> resourceTypeIds, URI relevance,
                                                                        String language, boolean includeMetadata) {
        final var subject = domainEntityHelperService.getSubjectByPublicId(subjectPublicId);

        final var subjectTopicTree = recursiveTopicTreeService.getRecursiveTopics(subject);

        final var topicIds = recursiveTopicTreeService.getRecursiveTopics(subject)
                .stream()
                .map(RecursiveTopicTreeService.TopicTreeElement::getTopicId)
                .collect(Collectors.toSet());

        // Populate a tree of subject->topic relations, add the resources to the list and then run sort on the list so all
        // levels are sorted on rank and relation type

        final Set<TopicResourceTreeSortable> resourcesToSort = new HashSet<>();

        subjectTopicTree.forEach(treeElement -> {
            if (treeElement.getParentSubjectId().isPresent()) {
                // This is a subjectTopic connection
                resourcesToSort.add(new TopicResourceTreeSortable("topic", "subject", treeElement.getTopicId(), treeElement.getParentSubjectId().orElse(0), treeElement.getRank()));
            } else {
                // This is a topicSubtopic connection
                resourcesToSort.add(new TopicResourceTreeSortable("topic", "topic", treeElement.getTopicId(), treeElement.getParentTopicId().orElse(0), treeElement.getRank()));
            }
        });

        return filterTopicResourcesByIdsAndReturn(topicIds, filterIds, resourceTypeIds, relevance, resourcesToSort, language, includeMetadata);
    }


    @Override
    public List<ResourceWithTopicConnectionDTO> getResourcesByTopicId(URI topicId, Set<URI> filterIdsRequest, URI filterBySubjectId,
                                                                      Set<URI> resourceTypeIds, URI relevancePublicId, String languageCode,
                                                                      boolean recursive, boolean includeMetadata) {
        final var topic = domainEntityHelperService.getTopicByPublicId(topicId);

        final Set<Integer> topicIdsToSearchFor;
        final Set<URI> filterIds;

        // Add both topics and resourceTopics to a common list that will be sorted in a tree-structure based on rank at each level
        final Set<TopicResourceTreeSortable> resourcesToSort = new HashSet<>();

        // If subject ID is specified and no filter IDs is specified, the filters are replaced with all filters directly associated with requested subject
        if (filterIdsRequest.size() == 0 && filterBySubjectId != null) {
            filterIds = domainEntityHelperService.getSubjectByPublicId(filterBySubjectId)
                    .getFilters()
                    .stream()
                    .map(Filter::getPublicId)
                    .collect(Collectors.toSet());
        } else {
            filterIds = filterIdsRequest;
        }

        // Populate a list of topic IDs we are going to fetch first, and then fetch the actual topics later
        // This allows searching recursively without having to fetch the whole relation tree on each element in the
        // recursive logic. It is also necessary to have the tree information later for ordering the result
        if (recursive) {
            final var topicList = recursiveTopicTreeService.getRecursiveTopics(topic);

            topicList.forEach(topicTreeElement -> resourcesToSort.add(new TopicResourceTreeSortable("topic", "topic", topicTreeElement.getTopicId(), topicTreeElement.getParentTopicId().orElse(0), topicTreeElement.getRank())));

            topicIdsToSearchFor = topicList.stream()
                    .map(RecursiveTopicTreeService.TopicTreeElement::getTopicId)
                    .collect(Collectors.toSet());
        } else {
            topicIdsToSearchFor = Set.of(topic.getId());
        }

        return filterTopicResourcesByIdsAndReturn(topicIdsToSearchFor, filterIds, resourceTypeIds, relevancePublicId, resourcesToSort, languageCode, includeMetadata);
    }

    @Override
    public ResourceDTO getResourceByPublicId(@NotNull URI publicId, String languageCode, boolean includeMetadata) {
        final var resource = resourceRepository.findFirstByPublicIdIncludingCachedUrlsAndTranslations(publicId)
                .orElseThrow(() -> new NotFoundHttpResponseException("No such resource found"));

        return new ResourceDTO(metadataEntityWrapperService.wrapEntity(resource, includeMetadata), languageCode);
    }

    @Override
    public ResourceWithParentTopicsDTO getResourceWithParentTopicsByPublicId(@NotNull URI publicId, String languageCode) {
        final var resource = resourceRepository.findFirstByPublicIdIncludingCachedUrlsAndTranslations(publicId)
                .orElseThrow(() -> new NotFoundHttpResponseException("No such resource found"));

        return new ResourceWithParentTopicsDTO(resource, languageCode);
    }

    @Override
    public List<ResourceDTO> getResources(String languageCode, boolean includeMetadata) {
        return metadataEntityWrapperService.wrapEntities(resourceRepository.findAllIncludingCachedUrlsAndTranslations(), includeMetadata)
                .stream()
                .map(wrappedResource -> new ResourceDTO(wrappedResource, languageCode))
                .collect(Collectors.toList());
    }
}
