/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.repositories.NodeResourceRepository;
import no.ndla.taxonomy.repositories.ResourceRepository;
import no.ndla.taxonomy.rest.NotFoundHttpResponseException;
import no.ndla.taxonomy.service.dtos.ResourceDTO;
import no.ndla.taxonomy.service.dtos.ResourceWithNodeConnectionDTO;
import no.ndla.taxonomy.service.dtos.ResourceWithParentsDTO;
import no.ndla.taxonomy.service.dtos.SearchResultDTO;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import org.springframework.data.domain.PageRequest;
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
    private final DomainEntityHelperService domainEntityHelperService;
    private final NodeResourceRepository nodeResourceRepository;
    private final RecursiveNodeTreeService recursiveNodeTreeService;
    private final TreeSorter treeSorter;

    public ResourceServiceImpl(ResourceRepository resourceRepository, EntityConnectionService connectionService,
            DomainEntityHelperService domainEntityHelperService, NodeResourceRepository nodeResourceRepository,
            RecursiveNodeTreeService recursiveNodeTreeService, TreeSorter topicTreeSorter) {
        this.resourceRepository = resourceRepository;
        this.connectionService = connectionService;
        this.domainEntityHelperService = domainEntityHelperService;
        this.nodeResourceRepository = nodeResourceRepository;
        this.recursiveNodeTreeService = recursiveNodeTreeService;
        this.treeSorter = topicTreeSorter;
    }

    @Override
    @Transactional
    public void delete(URI id) {
        final var resourceToDelete = resourceRepository.findFirstByPublicId(id)
                .orElseThrow(() -> new NotFoundServiceException("Subject was not found"));

        // ATM resources can not have any children, but still implements the interface that could
        // have children
        connectionService.disconnectAllChildren(resourceToDelete);

        resourceRepository.delete(resourceToDelete);
        resourceRepository.flush();
    }

    private List<ResourceWithNodeConnectionDTO> filterNodeResourcesByIdsAndReturn(Set<Integer> nodeIds,
            Set<URI> resourceTypeIds, URI relevance, Set<ResourceTreeSortable<Node>> sortableListToAddTo,
            String languageCode) {
        final List<NodeResource> nodeResources;

        if (resourceTypeIds.size() > 0) {
            nodeResources = nodeResourceRepository
                    .findAllByNodeIdsAndResourceTypePublicIdsAndRelevancePublicIdIfNotNullIncludingRelationsForResourceDocuments(
                            nodeIds, resourceTypeIds, relevance);
        } else {
            var nodeResourcesStream = nodeResourceRepository
                    .findAllByNodeIdsIncludingRelationsForResourceDocuments(nodeIds).stream();
            if (relevance != null) {
                final var isRequestingCore = "urn:relevance:core".equals(relevance.toString());
                nodeResourcesStream = nodeResourcesStream.filter(nodeResource -> {
                    final var resource = nodeResource.getResource().orElse(null);
                    if (resource == null) {
                        return false;
                    }
                    final var rel = nodeResource.getRelevance().orElse(null);
                    if (rel != null) {
                        return rel.getPublicId().equals(relevance);
                    } else {
                        return isRequestingCore;
                    }
                });
            }
            nodeResources = nodeResourcesStream.collect(Collectors.toList());
        }

        nodeResources.forEach(nodeResource -> sortableListToAddTo.add(new ResourceTreeSortable<Node>(nodeResource)));

        // Sort the list, extract all the topicResource objects in between topics and return list of documents

        return treeSorter.sortList(sortableListToAddTo).stream().map(ResourceTreeSortable::getResourceConnection)
                .filter(Optional::isPresent).map(Optional::get)
                .map(wrappedNodeResource -> new ResourceWithNodeConnectionDTO((NodeResource) wrappedNodeResource,
                        languageCode))
                .collect(Collectors.toList());

    }

    @Override
    public List<ResourceWithNodeConnectionDTO> getResourcesByNodeId(URI nodePublicId, Set<URI> resourceTypeIds,
            URI relevancePublicId, String languageCode, boolean recursive) {
        final var node = domainEntityHelperService.getNodeByPublicId(nodePublicId);

        final Set<Integer> topicIdsToSearchFor;

        // Add both topics and resourceTopics to a common list that will be sorted in a tree-structure based on rank at
        // each level
        final Set<ResourceTreeSortable<Node>> resourcesToSort = new HashSet<>();

        // Populate a list of topic IDs we are going to fetch first, and then fetch the actual topics later
        // This allows searching recursively without having to fetch the whole relation tree on each element in the
        // recursive logic. It is also necessary to have the tree information later for ordering the result
        if (recursive) {
            final var nodeList = recursiveNodeTreeService.getRecursiveNodes(node);

            nodeList.forEach(treeElement -> resourcesToSort.add(new ResourceTreeSortable<Node>("node", "node",
                    treeElement.getId(), treeElement.getParentId().orElse(0), treeElement.getRank())));

            topicIdsToSearchFor = nodeList.stream().map(RecursiveNodeTreeService.TreeElement::getId)
                    .collect(Collectors.toSet());
        } else {
            topicIdsToSearchFor = Set.of(node.getId());
        }

        return filterNodeResourcesByIdsAndReturn(topicIdsToSearchFor, resourceTypeIds, relevancePublicId,
                resourcesToSort, languageCode);
    }

    @Override
    public ResourceDTO getResourceByPublicId(URI publicId, String languageCode) {
        final var resource = resourceRepository.findFirstByPublicIdIncludingCachedUrlsAndTranslations(publicId)
                .orElseThrow(() -> new NotFoundHttpResponseException("No such resource found"));

        return new ResourceDTO(resource, languageCode);
    }

    @Override
    public ResourceWithParentsDTO getResourceWithParentNodesByPublicId(URI publicId, String languageCode) {
        final var resource = resourceRepository.findFirstByPublicIdIncludingCachedUrlsAndTranslations(publicId)
                .orElseThrow(() -> new NotFoundHttpResponseException("No such resource found"));

        return new ResourceWithParentsDTO(resource, languageCode);
    }

    private List<ResourceDTO> createDto(List<Resource> resources, String languageCode) {
        return resources.stream().map(resource -> new ResourceDTO(resource, languageCode)).collect(Collectors.toList());
    }

    @Override
    public List<ResourceDTO> getResources(Optional<String> language, Optional<URI> contentUri,
            MetadataFilters metadataFilters) {
        final List<ResourceDTO> listToReturn = new ArrayList<>();
        if (metadataFilters.hasFilters()) {
            List<Integer> resourceIds = new ArrayList<>();
            if (metadataFilters.getVisible().isPresent()) {
                resourceIds.addAll(
                        resourceRepository.getAllResourceIdsWithMetadataVisible(metadataFilters.getVisible().get()));
            }
            if (metadataFilters.getKey().isPresent()) {
                var idsWithMetadataKey = resourceRepository
                        .getAllResourceIdsWithMetadataKey(metadataFilters.getKey().get());
                if (resourceIds.isEmpty()) {
                    resourceIds.addAll(idsWithMetadataKey);
                } else {
                    resourceIds = resourceIds.stream().filter(idsWithMetadataKey::contains)
                            .collect(Collectors.toList());
                }
            }
            if (metadataFilters.getValue().isPresent()) {
                var idsWithMetadataValue = resourceRepository
                        .getAllResourceIdsWithMetadataValue(metadataFilters.getValue().get());
                if (resourceIds.isEmpty()) {
                    resourceIds.addAll(idsWithMetadataValue);
                } else {
                    resourceIds = resourceIds.stream().filter(idsWithMetadataValue::contains)
                            .collect(Collectors.toList());
                }
            }
            final var counter = new AtomicInteger();
            resourceIds.stream().collect(Collectors.groupingBy(i -> counter.getAndIncrement() / 1000)).values()
                    .forEach(idChunk -> {
                        final var resources = resourceRepository
                                .findByIdIncludingCachedUrlsAndResourceTypesAndFiltersAndTranslations(idChunk);
                        listToReturn.addAll(createDto(resources, language.get()));
                    });

        } else if (contentUri.isPresent()) {
            listToReturn.addAll(
                    createDto(resourceRepository.findByContentUriIncludingCachedUrlsAndTranslations(contentUri.get()),
                            language.get()));
        } else {
            final var allResourceIds = resourceRepository.getAllResourceIds();
            final var counter = new AtomicInteger();
            allResourceIds.stream().collect(Collectors.groupingBy(i -> counter.getAndIncrement() / 1000)).values()
                    .forEach(idChunk -> {
                        final var resources = resourceRepository
                                .findByIdIncludingCachedUrlsAndResourceTypesAndFiltersAndTranslations(idChunk);
                        listToReturn.addAll(createDto(resources, language.get()));
                    });
        }
        return listToReturn;
    }

    @Override
    public SearchResultDTO<ResourceDTO> searchResources(Optional<String> query, Optional<String> language, int pageSize,
            int page) {
        if (page < 1)
            throw new IllegalArgumentException("page parameter must be bigger than 0");

        var pageRequest = PageRequest.of(page - 1, pageSize);
        var fetched = query.isPresent()
                ? resourceRepository.searchAllByNameContainingIgnoreCase(query.get(), pageRequest)
                : resourceRepository.findAll(pageRequest);

        var languageCode = language.orElse("");

        var dtos = fetched.get().map(r -> new ResourceDTO(r, languageCode)).collect(Collectors.toList());

        return new SearchResultDTO<>(fetched.getTotalElements(), page, pageSize, dtos);
    }
}
