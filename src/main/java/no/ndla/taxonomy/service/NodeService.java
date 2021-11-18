/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeConnection;
import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.repositories.NodeConnectionRepository;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.service.dtos.*;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Transactional(readOnly = true)
@Service
public class NodeService {
    private final NodeRepository nodeRepository;
    private final NodeConnectionRepository nodeConnectionRepository;
    private final EntityConnectionService connectionService;
    private final MetadataApiService metadataApiService;
    private final TreeSorter topicTreeSorter;

    public NodeService(NodeRepository nodeRepository, NodeConnectionRepository nodeConnectionRepository,
            EntityConnectionService connectionService, MetadataApiService metadataApiService,
            TreeSorter topicTreeSorter) {
        this.nodeRepository = nodeRepository;
        this.nodeConnectionRepository = nodeConnectionRepository;
        this.connectionService = connectionService;
        this.metadataApiService = metadataApiService;
        this.topicTreeSorter = topicTreeSorter;
    }

    @Transactional
    public void delete(URI publicId, String versionHash) {
        final var nodeToDelete = nodeRepository.findFirstByPublicIdAndVersion(publicId, versionHash)
                .orElseThrow(() -> new NotFoundServiceException("Node was not found"));

        connectionService.disconnectAllChildren(nodeToDelete);
        nodeToDelete.setVersion(null);

        nodeRepository.delete(nodeToDelete);
        nodeRepository.flush();

        metadataApiService.deleteMetadataByPublicId(publicId);
    }

    @InjectMetadata
    public List<EntityWithPathDTO> getNodes(String versionHash, String languageCode, NodeType nodeTypeFilter,
            URI contentUriFilter, boolean isRoot) {
        final List<Node> filtered;

        if (isRoot) {
            filtered = nodeRepository.findAllRootsForVersionIncludingCachedUrlsAndTranslations(versionHash);
        } else {
            if (contentUriFilter != null && nodeTypeFilter != null) {
                filtered = nodeRepository.findAllByContentUriAndNodeTypeIncludingCachedUrlsAndTranslations(
                        contentUriFilter, nodeTypeFilter);
            } else if (contentUriFilter != null) {
                filtered = nodeRepository.findAllByContentUriIncludingCachedUrlsAndTranslations(contentUriFilter);
            } else if (nodeTypeFilter != null) {
                filtered = nodeRepository.findAllByNodeTypeIncludingCachedUrlsAndTranslations(nodeTypeFilter);
            } else {
                filtered = nodeRepository.findAllIncludingCachedUrlsAndTranslations();
            }
        }

        return filtered.stream().filter(node -> !isRoot || node.getParentNode().isEmpty())
                .map(node -> new NodeDTO(node, languageCode)).collect(Collectors.toList());
    }

    @MetadataQuery
    public List<EntityWithPathDTO> getNodes(String versionHash, String languageCode, NodeType nodeTypeFilter,
            URI contentUriFilter, MetadataKeyValueQuery metadataKeyValueQuery) {
        Set<String> publicIds = metadataKeyValueQuery.getDtos().stream().map(MetadataDto::getPublicId)
                .collect(Collectors.toSet());
        return publicIds.stream().map(topicId -> {
            try {
                return new URI(topicId);
            } catch (Exception e) {
                return null;
            }
        }).filter(Objects::nonNull).map(nodeRepository::findByPublicId).filter(Objects::nonNull)
                .filter(node -> node.getVersion().getHash().equals(versionHash)).filter(node -> {
                    /*
                     * I don't think this combination of queries will be normal, but it's easy to implement something
                     * that probably works.
                     */
                    if (contentUriFilter == null) {
                        return true;
                    } else {
                        return contentUriFilter.equals(node.getContentUri());
                    }
                }).filter(node -> {
                    /*
                     * I don't think this combination of queries will be normal, but it's easy to implement something
                     * that probably works.
                     */
                    if (nodeTypeFilter == null) {
                        return true;
                    } else {
                        return nodeTypeFilter.equals(node.getNodeType());
                    }
                }).map(node -> new NodeDTO(node, languageCode)).collect(Collectors.toList());
    }

    @InjectMetadata
    public List<ConnectionIndexDTO> getAllConnections(URI nodePublicId, String versionHash) {
        final var node = nodeRepository.findFirstByPublicIdAndVersion(nodePublicId, versionHash)
                .orElseThrow(() -> new NotFoundServiceException("Node was not found"));

        return Stream
                .concat(connectionService.getParentConnections(node).stream().map(ConnectionIndexDTO::parentConnection),
                        connectionService.getChildConnections(node).stream()
                                .filter(entity -> entity instanceof NodeConnection)
                                .map(ConnectionIndexDTO::childConnection))
                .collect(Collectors.toUnmodifiableList());
    }

    @InjectMetadata
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
