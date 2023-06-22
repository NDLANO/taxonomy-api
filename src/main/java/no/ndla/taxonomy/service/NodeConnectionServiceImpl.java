/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeConnection;
import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.domain.Relevance;
import no.ndla.taxonomy.repositories.NodeConnectionRepository;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.rest.NotFoundHttpResponseException;
import no.ndla.taxonomy.service.exceptions.DuplicateConnectionException;
import no.ndla.taxonomy.service.exceptions.InvalidArgumentServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(propagation = Propagation.MANDATORY)
@Service
public class NodeConnectionServiceImpl implements NodeConnectionService {
    private final NodeConnectionRepository nodeConnectionRepository;
    private final ContextUpdaterService cachedUrlUpdaterService;
    private final NodeRepository nodeRepository;

    public NodeConnectionServiceImpl(
            NodeConnectionRepository nodeConnectionRepository,
            ContextUpdaterService cachedUrlUpdaterService,
            NodeRepository nodeRepository) {
        this.nodeConnectionRepository = nodeConnectionRepository;
        this.cachedUrlUpdaterService = cachedUrlUpdaterService;
        this.nodeRepository = nodeRepository;
    }

    private NodeConnection doCreateConnection(
            Node parent, Node child, boolean requestedPrimary, Relevance relevance, int rank) {
        if (child.getParentConnections().size() == 0) {
            // First connected is always primary regardless of request
            requestedPrimary = true;
        }

        NodeConnection connection;

        if (parent != null) {
            connection = NodeConnection.create(parent, child);
        } else {
            throw new IllegalArgumentException("Unknown parent-child connection");
        }

        try {
            updatePrimaryConnection(connection, requestedPrimary);
        } catch (InvalidArgumentServiceException e) {
            // Only if setting the first node to non-primary, which we don't because we force
            // the first node to always become primary
            throw new RuntimeException(e);
        }

        updateRank(connection, rank);
        updateRelevance(connection, relevance);

        cachedUrlUpdaterService.updateContexts(child);

        return connection;
    }

    private NodeConnection createConnection(
            Node parent, Node child, Relevance relevance, int rank, Optional<Boolean> isPrimary) {
        return doCreateConnection(parent, child, isPrimary.orElse(true), relevance, rank);
    }

    public NodeConnection connectParentChild(Node parent, Node child, Relevance relevance, Integer rank) {
        return this.connectParentChild(parent, child, relevance, rank, Optional.empty());
    }

    @Override
    public NodeConnection connectParentChild(
            Node parent, Node child, Relevance relevance, Integer rank, Optional<Boolean> isPrimary) {
        if (child.getParentConnections().size() > 0) {
            if (child.getNodeType() == NodeType.TOPIC) throw new DuplicateConnectionException();

            var alreadyConnectedResource = parent.getResourceChildren().stream()
                    .anyMatch(connection -> connection.getChild().orElse(null) == child);

            if (alreadyConnectedResource) {
                throw new DuplicateConnectionException();
            }
        }

        if (parent == child) {
            throw new InvalidArgumentServiceException("Cannot connect node to itself");
        }

        Node parentConnected = parent;

        var ttl = 100;
        while (parentConnected.getParentConnections().stream()
                .findFirst()
                .map(NodeConnection::getParent)
                .isPresent()) {
            Logger.getLogger(this.getClass().toString())
                    .info(parentConnected.getPublicId().toString());
            parentConnected = parentConnected.getParentConnections().stream()
                    .findFirst()
                    .orElseThrow()
                    .getParent()
                    .orElseThrow();

            if (ttl-- < 0) {
                throw new InvalidArgumentServiceException("Too many levels to get top level object");
            }
            if (parentConnected == child) {
                throw new InvalidArgumentServiceException("Loop detected when trying to connect");
            }
        }

        if (rank == null) {
            rank = parent.getChildConnections().stream()
                            .map(NodeConnection::getRank)
                            .max(Integer::compare)
                            .orElse(0)
                    + 1;
        }

        return nodeConnectionRepository.saveAndFlush(createConnection(parent, child, relevance, rank, isPrimary));
    }

    @Override
    public void disconnectParentChild(Node parent, Node child) {
        new HashSet<>(parent.getChildConnections())
                .stream()
                        .filter(connection -> connection.getChild().orElse(null) == child)
                        .forEach(this::disconnectParentChildConnection); // (It will never be more than one record)
    }

    @Override
    public void disconnectParentChildConnection(NodeConnection nodeConnection) {
        final var child = nodeConnection.getChild();

        nodeConnection.disassociate();
        nodeConnectionRepository.delete(nodeConnection);

        child.ifPresent(childToDisconnect -> {
            if (childToDisconnect.getNodeType() == NodeType.RESOURCE) {
                // Set next connection to primary if disconnecting the primary connection
                var isPrimaryConnection = nodeConnection.isPrimary().orElse(false);
                if (isPrimaryConnection) {
                    childToDisconnect.getParentConnections().stream()
                            .findFirst()
                            .ifPresent(nextConnection -> {
                                nextConnection.setPrimary(true);
                                nodeConnectionRepository.saveAndFlush(nextConnection);
                                nextConnection.getResource().ifPresent(cachedUrlUpdaterService::updateContexts);
                            });
                }
            }
            cachedUrlUpdaterService.updateContexts(childToDisconnect);
        });

        nodeConnectionRepository.flush();
    }

    private void saveConnections(Collection<NodeConnection> connections) {
        connections.forEach(nodeConnectionRepository::save);
        nodeConnectionRepository.flush();
    }

    private void updatePrimaryConnection(NodeConnection connectable, boolean setPrimaryTo) {
        final var updatedConnectables = new HashSet<NodeConnection>();
        updatedConnectables.add(connectable);

        // Updates all other nodes connected to this parent
        final var foundNewPrimary = new AtomicBoolean(false);
        connectable.getChild().ifPresent(node -> node.getParentConnections().stream()
                .filter(connectable1 -> connectable1 != connectable)
                .forEachOrdered(connectable1 -> {
                    if (!setPrimaryTo && !foundNewPrimary.get()) {
                        connectable1.setPrimary(true);
                        foundNewPrimary.set(true);
                        updatedConnectables.add(connectable1);
                    } else if (setPrimaryTo) {
                        connectable1.setPrimary(false);
                        updatedConnectables.add(connectable1);
                    }
                }));

        connectable.setPrimary(setPrimaryTo);

        saveConnections(updatedConnectables);

        updatedConnectables.forEach(
                updatedConnectable -> updatedConnectable.getChild().ifPresent(cachedUrlUpdaterService::updateContexts));

        if (!setPrimaryTo && !foundNewPrimary.get()) {
            throw new InvalidArgumentServiceException(
                    "Requested to set non-primary, but cannot find another node to set primary");
        }
    }

    private void updateRank(NodeConnection rankable, int newRank) {
        final var updatedConnections = RankableConnectionUpdater.rank(
                new ArrayList<>(rankable.getParent()
                        .orElseThrow(() -> new IllegalStateException("Rankable parent not found"))
                        .getChildConnections()),
                rankable,
                newRank);
        saveConnections(updatedConnections);
    }

    private void updateRelevance(NodeConnection connection, Relevance relevance) {
        connection.setRelevance(relevance);

        this.saveConnections(Collections.singletonList(connection));
    }

    private void updateRankableConnection(NodeConnection connection, boolean isPrimary, Integer newRank) {
        updatePrimaryConnection(connection, isPrimary);

        if (newRank != null) {
            updateRank(connection, newRank);
        }
    }

    @Override
    public void updateParentChild(
            NodeConnection nodeConnection,
            Relevance relevance,
            Optional<Integer> newRank,
            Optional<Boolean> isPrimary) {
        newRank.ifPresent(integer -> updateRank(nodeConnection, integer));
        isPrimary.ifPresent(primary -> updatePrimaryConnection(nodeConnection, primary));
        updateRelevance(nodeConnection, relevance);
    }

    @Override
    public void replacePrimaryConnectionsFor(Node entity) {
        entity.getChildConnections().stream()
                .filter(connection -> connection.isPrimary().orElse(false))
                .forEach(connection -> {
                    try {
                        updatePrimaryConnection(connection, false);
                    } catch (InvalidArgumentServiceException ignored) {
                    }
                });
    }

    @Override
    public Collection<NodeConnection> getParentConnections(Node entity) {
        // (applies to both getChildConnections and getParentConnections)
        //
        // While this method will work on any objects implementing the EntityWithPath interface
        // there is an
        // optimized path for Topic objects that will perform better than just reading from the
        // probably
        // lazy initialized properties of the object

        return entity.getParentConnections();
    }

    @Override
    public Collection<NodeConnection> getChildConnections(Node entity) {
        return entity.getChildConnections();
    }

    @Override
    public void disconnectAllParents(URI nodeId) {
        var node = nodeRepository
                .findFirstByPublicId(nodeId)
                .orElseThrow(() -> new NotFoundHttpResponseException("Node was not found"));
        node.getParentConnections().forEach(this::disconnectParentChildConnection);
    }

    @Override
    public void disconnectAllChildren(Node entity) {
        Set.copyOf(entity.getChildConnections()).forEach(this::disconnectParentChildConnection);
    }
}
