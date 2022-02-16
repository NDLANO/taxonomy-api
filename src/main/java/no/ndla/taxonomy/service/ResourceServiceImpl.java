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
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Join;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.data.jpa.domain.Specification.where;

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

    public Specification<Resource> base() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isNotNull(root.get("id"));
    }

    public Specification<Resource> resourceIsVisible() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("metadata").get("visible"), true);
    }

    public Specification<Resource> resourceHasContentUri(URI contentUri) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("contentUri"), contentUri);
    }

    public Specification<Resource> resourceHasCustomKey(String key) {
        return (root, query, criteriaBuilder) -> {
            Join<Resource, Metadata> resourceMetadataJoin = root.join("metadata");
            Join<Metadata, CustomFieldValue> join = resourceMetadataJoin.join("customFieldValues");
            return criteriaBuilder.equal(join.get("customField").get("key"), key);
        };
    }

    public Specification<Resource> resourceHasCustomValue(String value) {
        return (root, query, criteriaBuilder) -> {
            Join<Resource, Metadata> resourceMetadataJoin = root.join("metadata");
            Join<Metadata, CustomFieldValue> join = resourceMetadataJoin.join("customFieldValues");
            return criteriaBuilder.equal(join.get("value"), value);
        };
    }

    @Override
    public List<ResourceDTO> getResources(Optional<String> language, Optional<URI> contentUri,
            MetadataFilters metadataFilters) {
        final List<Resource> filtered;
        Specification<Resource> specification = where(base());
        if (contentUri.isPresent()) {
            specification = specification.and(resourceHasContentUri(contentUri.get()));
        }
        if (metadataFilters.getVisible().isPresent()) {
            specification = specification.and(resourceIsVisible());
        }
        if (metadataFilters.getKey().isPresent()) {
            specification = specification.and(resourceHasCustomKey(metadataFilters.getKey().get()));
        }
        if (metadataFilters.getValue().isPresent()) {
            specification = specification.and(resourceHasCustomValue(metadataFilters.getValue().get()));
        }
        filtered = resourceRepository.findAll(specification);

        return createDto(filtered, language.get());
    }
}
