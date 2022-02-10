/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.repositories.NodeConnectionRepository;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.service.dtos.*;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Join;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.data.jpa.domain.Specification.where;

@Transactional(readOnly = true)
@Service
public class NodeService {
    private final NodeRepository nodeRepository;
    private final NodeConnectionRepository nodeConnectionRepository;
    private final EntityConnectionService connectionService;
    private final TreeSorter topicTreeSorter;

    public NodeService(NodeRepository nodeRepository, NodeConnectionRepository nodeConnectionRepository,
            EntityConnectionService connectionService, TreeSorter topicTreeSorter) {
        this.nodeRepository = nodeRepository;
        this.nodeConnectionRepository = nodeConnectionRepository;
        this.connectionService = connectionService;
        this.topicTreeSorter = topicTreeSorter;
    }

    @Transactional
    public void delete(URI publicId) {
        final var nodeToDelete = nodeRepository.findFirstByPublicId(publicId)
                .orElseThrow(() -> new NotFoundServiceException("Node was not found"));

        connectionService.disconnectAllChildren(nodeToDelete);

        nodeRepository.delete(nodeToDelete);
        nodeRepository.flush();
    }

    public Specification<Node> base() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isNotNull(root.get("id"));
    }

    public Specification<Node> nodeIsRoot() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("root"), true);
    }

    public Specification<Node> nodeIsVisible() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("metadata").get("visible"), true);
    }

    public Specification<Node> nodeHasNodeType(NodeType nodeType) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("nodeType"), nodeType);
    }

    public Specification<Node> nodeHasContentUri(URI contentUri) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("contentUri"), contentUri);
    }

    public Specification<Node> nodeHasCustomKey(String key) {
        return (root, query, criteriaBuilder) -> {
            Join<Node, Metadata> nodeMetadataJoin = root.join("metadata");
            Join<Metadata, CustomFieldValue> join = nodeMetadataJoin.join("customFieldValues");
            return criteriaBuilder.equal(join.get("customField").get("key"), key);
        };
    }

    public Specification<Node> nodeHasCustomValue(String value) {
        return (root, query, criteriaBuilder) -> {
            Join<Node, Metadata> nodeMetadataJoin = root.join("metadata");
            Join<Metadata, CustomFieldValue> join = nodeMetadataJoin.join("customFieldValues");
            return criteriaBuilder.equal(join.get("value"), value);
        };
    }

    public List<EntityWithPathDTO> getNodes(Optional<String> language, Optional<NodeType> nodeType,
            Optional<URI> contentUri, Optional<Boolean> isRoot, MetadataFilters metadataFilters) {

        final List<Node> filtered;
        Specification<Node> specification = where(base());
        if (isRoot.isPresent()) {
            specification = specification.and(nodeIsRoot());
        }
        if (contentUri.isPresent()) {
            specification = specification.and(nodeHasContentUri(contentUri.get()));
        }
        if (nodeType.isPresent()) {
            specification = specification.and(nodeHasNodeType(nodeType.get()));
        }
        if (metadataFilters.getVisible().isPresent()) {
            specification = specification.and(nodeIsVisible());
        }
        if (metadataFilters.getKey().isPresent()) {
            specification = specification.and(nodeHasCustomKey(metadataFilters.getKey().get()));
        }
        if (metadataFilters.getValue().isPresent()) {
            specification = specification.and(nodeHasCustomValue(metadataFilters.getValue().get()));
        }
        filtered = nodeRepository.findAll(specification);

        return filtered.stream().map(n -> new NodeDTO(n, language.get())).collect(Collectors.toList());
    }

    public List<EntityWithPathDTO> getNodes(String languageCode, NodeType nodeTypeFilter, URI contentUriFilter,
            MetadataKeyValueQuery metadataKeyValueQuery) {
        Set<String> publicIds = metadataKeyValueQuery.getDtos().stream().map(MetadataDto::getPublicId)
                .collect(Collectors.toSet());
        return publicIds.stream().map(topicId -> {
            try {
                return new URI(topicId);
            } catch (Exception e) {
                return null;
            }
        }).filter(Objects::nonNull).map(nodeRepository::findByPublicId).filter(Objects::nonNull).filter(node -> {
            /*
             * I don't think this combination of queries will be normal, but it's easy to implement something that
             * probably works.
             */
            if (contentUriFilter == null) {
                return true;
            } else {
                return contentUriFilter.equals(node.getContentUri());
            }
        }).filter(node -> {
            /*
             * I don't think this combination of queries will be normal, but it's easy to implement something that
             * probably works.
             */
            if (nodeTypeFilter == null) {
                return true;
            } else {
                return nodeTypeFilter.equals(node.getNodeType());
            }
        }).map(node -> new NodeDTO(node, languageCode)).collect(Collectors.toList());
    }

    public List<ConnectionIndexDTO> getAllConnections(URI nodePublicId) {
        final var node = nodeRepository.findFirstByPublicId(nodePublicId)
                .orElseThrow(() -> new NotFoundServiceException("Node was not found"));

        return Stream
                .concat(connectionService.getParentConnections(node).stream().map(ConnectionIndexDTO::parentConnection),
                        connectionService.getChildConnections(node).stream()
                                .filter(entity -> entity instanceof NodeConnection)
                                .map(ConnectionIndexDTO::childConnection))
                .collect(Collectors.toUnmodifiableList());
    }

    public List<TopicChildDTO> getFilteredChildConnections(URI nodePublicId, String languageCode) {
        final var node = nodeRepository.findFirstByPublicId(nodePublicId)
                .orElseThrow(() -> new NotFoundServiceException("Node was not found"));
        final List<NodeConnection> childConnections = nodeConnectionRepository
                .findAllByParentPublicIdIncludingChildAndChildTranslations(nodePublicId);

        final var wrappedList = childConnections.stream()
                .map(nodeConnection -> new TopicChildDTO(node, nodeConnection, languageCode))
                .collect(Collectors.toUnmodifiableList());

        return topicTreeSorter.sortList(wrappedList);
    }
}
