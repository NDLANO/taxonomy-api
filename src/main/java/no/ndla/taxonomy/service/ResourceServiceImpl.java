/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.DomainEntity;
import no.ndla.taxonomy.domain.Resource;
import no.ndla.taxonomy.domain.TopicResource;
import no.ndla.taxonomy.repositories.ResourceRepository;
import no.ndla.taxonomy.repositories.TopicResourceRepository;
import no.ndla.taxonomy.rest.NotFoundHttpResponseException;
import no.ndla.taxonomy.service.dtos.MetadataDto;
import no.ndla.taxonomy.service.dtos.ResourceDTO;
import no.ndla.taxonomy.service.dtos.ResourceWithParentTopicsDTO;
import no.ndla.taxonomy.service.dtos.ResourceWithTopicConnectionDTO;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
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

    public ResourceServiceImpl(
            ResourceRepository resourceRepository,
            TopicResourceRepository topicResourceRepository,
            EntityConnectionService connectionService,
            MetadataApiService metadataApiService,
            DomainEntityHelperService domainEntityHelperService,
            RecursiveTopicTreeService recursiveTopicTreeService,
            TopicTreeSorter topicTreeSorter) {
        this.resourceRepository = resourceRepository;
        this.connectionService = connectionService;
        this.metadataApiService = metadataApiService;
        this.domainEntityHelperService = domainEntityHelperService;
        this.recursiveTopicTreeService = recursiveTopicTreeService;
        this.topicResourceRepository = topicResourceRepository;
        this.topicTreeSorter = topicTreeSorter;
    }

    @Override
    @Transactional
    public void delete(URI id) {
        final var resourceToDelete =
                resourceRepository
                        .findFirstByPublicId(id)
                        .orElseThrow(() -> new NotFoundServiceException("Subject was not found"));

        // ATM resources can not have any children, but still implements the interface that could
        // have children
        connectionService.disconnectAllChildren(resourceToDelete);

        resourceRepository.delete(resourceToDelete);
        resourceRepository.flush();

        metadataApiService.deleteMetadataByPublicId(id);
    }

    private List<ResourceWithTopicConnectionDTO> filterTopicResourcesByIdsAndReturn(
            Set<Integer> topicIds,
            Set<URI> resourceTypeIds,
            URI relevance,
            Set<TopicResourceTreeSortable> sortableListToAddTo,
            String languageCode) {
        final List<TopicResource> topicResources;

        if (resourceTypeIds.size() > 0) {
            topicResources =
                    topicResourceRepository
                            .findAllByTopicIdsAndResourceTypePublicIdsAndRelevancePublicIdIfNotNullIncludingRelationsForResourceDocuments(
                                    topicIds, resourceTypeIds, relevance);
        } else {
            var topicResourcesStream =
                    topicResourceRepository
                            .findAllByTopicIdsIncludingRelationsForResourceDocuments(topicIds)
                            .stream();
            if (relevance != null) {
                final var isRequestingCore = "urn:relevance:core".equals(relevance.toString());
                topicResourcesStream =
                        topicResourcesStream.filter(
                                topicResource -> {
                                    final var resource = topicResource.getResource().orElse(null);
                                    if (resource == null) {
                                        return false;
                                    }
                                    final var rel = topicResource.getRelevance().orElse(null);
                                    if (rel != null) {
                                        return rel.getPublicId().equals(relevance);
                                    } else {
                                        return isRequestingCore;
                                    }
                                });
            }
            topicResources = topicResourcesStream.collect(Collectors.toList());
        }

        topicResources.forEach(
                topicResource ->
                        sortableListToAddTo.add(new TopicResourceTreeSortable(topicResource)));

        // Sort the list, extract all the topicResource objects in between topics and return list of
        // documents

        return topicTreeSorter.sortList(sortableListToAddTo).stream()
                .map(TopicResourceTreeSortable::getTopicResource)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(
                        wrappedTopicResource ->
                                new ResourceWithTopicConnectionDTO(
                                        wrappedTopicResource, languageCode))
                .collect(Collectors.toList());
    }

    @Override
    @InjectMetadata
    public List<ResourceWithTopicConnectionDTO> getResourcesBySubjectId(
            URI subjectPublicId, Set<URI> resourceTypeIds, URI relevance, String language) {
        final var subject = domainEntityHelperService.getSubjectByPublicId(subjectPublicId);

        final var subjectTopicTree = recursiveTopicTreeService.getRecursiveTopics(subject);

        final var topicIds =
                recursiveTopicTreeService.getRecursiveTopics(subject).stream()
                        .map(RecursiveTopicTreeService.TopicTreeElement::getTopicId)
                        .collect(Collectors.toSet());

        // Populate a tree of subject->topic relations, add the resources to the list and then run
        // sort on the list so all
        // levels are sorted on rank and relation type

        final Set<TopicResourceTreeSortable> resourcesToSort = new HashSet<>();

        subjectTopicTree.forEach(
                treeElement -> {
                    if (treeElement.getParentSubjectId().isPresent()) {
                        // This is a subjectTopic connection
                        resourcesToSort.add(
                                new TopicResourceTreeSortable(
                                        "topic",
                                        "subject",
                                        treeElement.getTopicId(),
                                        treeElement.getParentSubjectId().orElse(0),
                                        treeElement.getRank()));
                    } else {
                        // This is a topicSubtopic connection
                        resourcesToSort.add(
                                new TopicResourceTreeSortable(
                                        "topic",
                                        "topic",
                                        treeElement.getTopicId(),
                                        treeElement.getParentTopicId().orElse(0),
                                        treeElement.getRank()));
                    }
                });

        return filterTopicResourcesByIdsAndReturn(
                topicIds, resourceTypeIds, relevance, resourcesToSort, language);
    }

    @Override
    @InjectMetadata
    public List<ResourceWithTopicConnectionDTO> getResourcesByTopicId(
            URI topicId,
            URI filterBySubjectId,
            Set<URI> resourceTypeIds,
            URI relevancePublicId,
            String languageCode,
            boolean recursive) {
        final var topic = domainEntityHelperService.getTopicByPublicId(topicId);

        final Set<Integer> topicIdsToSearchFor;

        // Add both topics and resourceTopics to a common list that will be sorted in a
        // tree-structure based on rank at each level
        final Set<TopicResourceTreeSortable> resourcesToSort = new HashSet<>();

        // Populate a list of topic IDs we are going to fetch first, and then fetch the actual
        // topics later
        // This allows searching recursively without having to fetch the whole relation tree on each
        // element in the
        // recursive logic. It is also necessary to have the tree information later for ordering the
        // result
        if (recursive) {
            final var topicList = recursiveTopicTreeService.getRecursiveTopics(topic);

            topicList.forEach(
                    topicTreeElement ->
                            resourcesToSort.add(
                                    new TopicResourceTreeSortable(
                                            "topic",
                                            "topic",
                                            topicTreeElement.getTopicId(),
                                            topicTreeElement.getParentTopicId().orElse(0),
                                            topicTreeElement.getRank())));

            topicIdsToSearchFor =
                    topicList.stream()
                            .map(RecursiveTopicTreeService.TopicTreeElement::getTopicId)
                            .collect(Collectors.toSet());
        } else {
            topicIdsToSearchFor = Set.of(topic.getId());
        }

        return filterTopicResourcesByIdsAndReturn(
                topicIdsToSearchFor,
                resourceTypeIds,
                relevancePublicId,
                resourcesToSort,
                languageCode);
    }

    @Override
    @InjectMetadata
    public ResourceDTO getResourceByPublicId(URI publicId, String languageCode) {
        final var resource =
                resourceRepository
                        .findFirstByPublicIdIncludingCachedUrlsAndTranslations(publicId)
                        .orElseThrow(
                                () -> new NotFoundHttpResponseException("No such resource found"));

        return new ResourceDTO(resource, languageCode);
    }

    @Override
    @InjectMetadata
    public ResourceWithParentTopicsDTO getResourceWithParentTopicsByPublicId(
            URI publicId, String languageCode) {
        final var resource =
                resourceRepository
                        .findFirstByPublicIdIncludingCachedUrlsAndTranslations(publicId)
                        .orElseThrow(
                                () -> new NotFoundHttpResponseException("No such resource found"));

        return new ResourceWithParentTopicsDTO(resource, languageCode);
    }

    private List<ResourceDTO> createDto(List<Resource> resources, String languageCode) {
        return resources.stream()
                .map(resource -> new ResourceDTO(resource, languageCode))
                .collect(Collectors.toList());
    }

    @Override
    @InjectMetadata
    public List<ResourceDTO> getResources(String languageCode, URI contentUriFilter) {
        final List<ResourceDTO> listToReturn = new ArrayList<>();

        if (contentUriFilter != null) {
            return createDto(
                    resourceRepository
                            .findAllByContentUriIncludingCachedUrlsAndResourceTypesAndFiltersAndTranslations(
                                    contentUriFilter),
                    languageCode);
        } else {
            // Get all resource ids and chunk it in requests with all relations small enough to not
            // create a huge heap space usage peak

            final var allResourceIds = resourceRepository.getAllResourceIds();

            final var counter = new AtomicInteger();

            allResourceIds.stream()
                    .collect(Collectors.groupingBy(i -> counter.getAndIncrement() / 1000))
                    .values()
                    .forEach(
                            idChunk -> {
                                final var resources =
                                        resourceRepository
                                                .findByIdIncludingCachedUrlsAndResourceTypesAndFiltersAndTranslations(
                                                        idChunk);
                                listToReturn.addAll(createDto(resources, languageCode));
                            });
        }

        return listToReturn;
    }

    @Override
    @MetadataQuery
    public List<ResourceDTO> getResources(
            String languageCode,
            URI contentUriFilter,
            MetadataKeyValueQuery metadataKeyValueQuery) {
        Set<String> publicIds =
                metadataKeyValueQuery.getDtos().stream()
                        .map(MetadataDto::getPublicId)
                        .collect(Collectors.toSet());

        final var counter = new AtomicInteger();
        return publicIds.stream()
                .map(
                        resourceId -> {
                            try {
                                return new URI(resourceId);
                            } catch (Exception e) {
                                return null;
                            }
                        })
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(i -> counter.getAndIncrement() / 1000))
                .values()
                .stream()
                .flatMap(
                        idChunk -> {
                            final var resources =
                                    resourceRepository
                                            .findByPublicIdIncludingCachedUrlsAndResourceTypesAndFiltersAndTranslations(
                                                    idChunk);
                            return createDto(resources, languageCode).stream();
                        })
                .filter(Objects::nonNull)
                .filter(
                        resource -> {
                            if (contentUriFilter == null) return true;
                            else return contentUriFilter.equals(resource.getContentUri());
                        })
                .collect(Collectors.toList());
    }
}
