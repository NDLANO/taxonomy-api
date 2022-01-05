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
import no.ndla.taxonomy.service.dtos.*;
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
    private final NodeResourceRepository nodeResourceRepository;
    private final RecursiveNodeTreeService recursiveNodeTreeService;
    private final TreeSorter treeSorter;

    public ResourceServiceImpl(ResourceRepository resourceRepository, EntityConnectionService connectionService,
            MetadataApiService metadataApiService, DomainEntityHelperService domainEntityHelperService,
            NodeResourceRepository nodeResourceRepository, RecursiveNodeTreeService recursiveNodeTreeService,
            TreeSorter topicTreeSorter) {
        this.resourceRepository = resourceRepository;
        this.connectionService = connectionService;
        this.metadataApiService = metadataApiService;
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

        metadataApiService.deleteMetadataByPublicId(id);
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
    @InjectMetadata
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
    @InjectMetadata
    public ResourceDTO getResourceByPublicId(URI publicId, String languageCode) {
        final var resource = resourceRepository.findFirstByPublicIdIncludingCachedUrlsAndTranslations(publicId)
                .orElseThrow(() -> new NotFoundHttpResponseException("No such resource found"));

        return new ResourceDTO(resource, languageCode);
    }

    @Override
    @InjectMetadata
    public ResourceWithParentsDTO getResourceWithParentNodesByPublicId(URI publicId, String languageCode) {
        final var resource = resourceRepository.findFirstByPublicIdIncludingCachedUrlsAndTranslations(publicId)
                .orElseThrow(() -> new NotFoundHttpResponseException("No such resource found"));

        return new ResourceWithParentsDTO(resource, languageCode);
    }

    private List<ResourceDTO> createDto(List<Resource> resources, String languageCode) {
        return resources.stream().map(resource -> new ResourceDTO(resource, languageCode)).collect(Collectors.toList());
    }

    @Override
    @InjectMetadata
    public List<ResourceDTO> getResources(String languageCode, URI contentUriFilter) {
        final List<ResourceDTO> listToReturn = new ArrayList<>();

        if (contentUriFilter != null) {
            return createDto(resourceRepository
                    .findAllByContentUriIncludingCachedUrlsAndResourceTypesAndFiltersAndTranslations(contentUriFilter),
                    languageCode);
        } else {
            // Get all resource ids and chunk it in requests with all relations small enough to not
            // create a huge heap space usage peak

            final var allResourceIds = resourceRepository.getAllResourceIds();

            final var counter = new AtomicInteger();

            allResourceIds.stream().collect(Collectors.groupingBy(i -> counter.getAndIncrement() / 1000)).values()
                    .forEach(idChunk -> {
                        final var resources = resourceRepository
                                .findByIdIncludingCachedUrlsAndResourceTypesAndFiltersAndTranslations(idChunk);
                        listToReturn.addAll(createDto(resources, languageCode));
                    });
        }

        return listToReturn;
    }

    @Override
    @MetadataQuery
    public List<ResourceDTO> getResources(String languageCode, URI contentUriFilter,
            MetadataKeyValueQuery metadataKeyValueQuery) {
        Set<String> publicIds = metadataKeyValueQuery.getDtos().stream().map(MetadataDto::getPublicId)
                .collect(Collectors.toSet());

        final var counter = new AtomicInteger();
        return publicIds.stream().map(resourceId -> {
            try {
                return new URI(resourceId);
            } catch (Exception e) {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.groupingBy(i -> counter.getAndIncrement() / 1000)).values()
                .stream().flatMap(idChunk -> {
                    final var resources = resourceRepository
                            .findByPublicIdIncludingCachedUrlsAndResourceTypesAndFiltersAndTranslations(idChunk);
                    return createDto(resources, languageCode).stream();
                }).filter(Objects::nonNull).filter(resource -> {
                    if (contentUriFilter == null)
                        return true;
                    else
                        return contentUriFilter.equals(resource.getContentUri());
                }).collect(Collectors.toList());
    }
}
