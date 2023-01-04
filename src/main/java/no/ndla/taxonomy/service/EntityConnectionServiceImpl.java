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
import no.ndla.taxonomy.repositories.NodeResourceRepository;
import no.ndla.taxonomy.rest.NotFoundHttpResponseException;
import no.ndla.taxonomy.service.exceptions.DuplicateConnectionException;
import no.ndla.taxonomy.service.exceptions.InvalidArgumentServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

@Transactional(propagation = Propagation.MANDATORY)
@Service
public class EntityConnectionServiceImpl implements EntityConnectionService {
    private final NodeConnectionRepository nodeConnectionRepository;
    private final CachedUrlUpdaterService cachedUrlUpdaterService;
    private final NodeRepository nodeRepository;

    public EntityConnectionServiceImpl(
            NodeConnectionRepository nodeConnectionRepository,
            CachedUrlUpdaterService cachedUrlUpdaterService,
            NodeRepository nodeRepository
    ) {
        this.nodeConnectionRepository = nodeConnectionRepository;
        this.cachedUrlUpdaterService = cachedUrlUpdaterService;
        this.nodeRepository = nodeRepository;
    }

    private EntityWithPathConnection doCreateConnection(EntityWithPath parent, EntityWithPath child,
                                                        boolean requestedPrimary, Relevance relevance, int rank) {
        if (child.getParentConnections().size() == 0) {
            // First connected is always primary regardless of request
            requestedPrimary = true;
        }

        EntityWithPathConnection connection;

        if (parent instanceof Node && child instanceof Node) {
            connection = NodeConnection.create((Node) parent, (Node) child);
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

        cachedUrlUpdaterService.updateCachedUrls(child);

        return connection;
    }

    private NodeConnection createConnection(Node parent, Node child, Relevance relevance, int rank, Optional<Boolean> isPrimary) {
        return (NodeConnection) doCreateConnection(parent, child, isPrimary.orElse(true), relevance, rank);
    }

    @Override
    public NodeConnection connectParentChild(Node parent, Node child, Relevance relevance, Integer rank, Optional<Boolean> isPrimary) {
        if (child.getParentConnections().size() > 0) {
            throw new DuplicateConnectionException();
        }

        if (parent == child) {
            throw new InvalidArgumentServiceException("Cannot connect node to itself");
        }

        if (child.isRoot()) {
            throw new InvalidArgumentServiceException("Root node cannot be set as child");
        }

        EntityWithPath parentConnected = parent;

        var ttl = 100;
        while (parentConnected.getParentConnections().stream().findFirst()
                .map(EntityWithPathConnection::getConnectedParent).isPresent()) {
            Logger.getLogger(this.getClass().toString()).info(parentConnected.getPublicId().toString());
            parentConnected = parentConnected.getParentConnections().stream().findFirst().orElseThrow()
                    .getConnectedParent().orElseThrow();

            if (ttl-- < 0) {
                throw new InvalidArgumentServiceException("Too many levels to get top level object");
            }
            if (parentConnected == child) {
                throw new InvalidArgumentServiceException("Loop detected when trying to connect");
            }
        }

        if (rank == null) {
            rank = parent.getChildren().stream().map(NodeConnection::getRank).max(Integer::compare).orElse(0) + 1;
        }

        return nodeConnectionRepository.saveAndFlush(createConnection(parent, child, relevance, rank, isPrimary));
    }

    @Override
    public void disconnectParentChild(Node parent, Node child) {
        new HashSet<>(parent.getChildren()).stream()
                .filter(connection -> connection.getConnectedChild().orElse(null) == child)
                .forEach(this::disconnectParentChildConnection); // (It will never be more than one record)
    }

    @Override
    public void disconnectParentChildConnection(NodeConnection nodeConnection) {
        final var child = nodeConnection.getChild();

        nodeConnection.disassociate();
        nodeConnectionRepository.delete(nodeConnection);

        child.ifPresent(cachedUrlUpdaterService::updateCachedUrls);

        nodeConnectionRepository.flush();
    }

    private void saveConnections(Collection<EntityWithPathConnection> connections) {
        connections.forEach(connectable -> {
            if (connectable instanceof NodeConnection) {
                nodeConnectionRepository.save((NodeConnection) connectable);
            } else {
                throw new IllegalArgumentException(
                        "Unknown instance of PrimaryPathConnectable: " + connectable.getClass().toString());
            }
        });
        nodeConnectionRepository.flush();
    }

    private void updatePrimaryConnection(EntityWithPathConnection connectable, boolean setPrimaryTo) {
        final var updatedConnectables = new HashSet<EntityWithPathConnection>();
        updatedConnectables.add(connectable);

        // Updates all other nodes connected to this parent
        final var foundNewPrimary = new AtomicBoolean(false);
        connectable.getConnectedChild().ifPresent(entityWithPath -> entityWithPath.getParentConnections().stream()
                .filter(connectable1 -> connectable1 != connectable).forEachOrdered(connectable1 -> {
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

        updatedConnectables.forEach(updatedConnectable -> updatedConnectable.getConnectedChild()
                .ifPresent(cachedUrlUpdaterService::updateCachedUrls));

        if (!setPrimaryTo && !foundNewPrimary.get()) {
            throw new InvalidArgumentServiceException(
                    "Requested to set non-primary, but cannot find another node to set primary");
        }
    }

    private void updateRank(EntityWithPathConnection rankable, int newRank) {
        final var updatedConnections = RankableConnectionUpdater.rank(new ArrayList<>(rankable.getConnectedParent()
                        .orElseThrow(() -> new IllegalStateException("Rankable parent not found")).getChildConnections()),
                rankable, newRank);
        saveConnections(updatedConnections);
    }

    private void updateRelevance(EntityWithPathConnection connection, Relevance relevance) {
        connection.setRelevance(relevance);

        this.saveConnections(Collections.singletonList(connection));
    }

    private void updateRankableConnection(EntityWithPathConnection connection, boolean isPrimary, Integer newRank) {
        updatePrimaryConnection(connection, isPrimary);

        if (newRank != null) {
            updateRank(connection, newRank);
        }
    }

    @Override
    public void updateParentChild(NodeConnection nodeConnection, Relevance relevance, Integer newRank, Optional<Boolean> isPrimary) {
        updateRank(nodeConnection, newRank);
        isPrimary.ifPresent(primary -> updatePrimaryConnection(nodeConnection, primary));
        updateRelevance(nodeConnection, relevance);
    }

    @Override
    public void replacePrimaryConnectionsFor(EntityWithPath entity) {
        entity.getChildConnections().stream().filter(connection -> connection.isPrimary().orElse(false))
                .forEach(connection -> {
                    try {
                        updatePrimaryConnection(connection, false);
                    } catch (InvalidArgumentServiceException ignored) {
                    }
                });
    }

    @Override
    public Collection<EntityWithPathConnection> getParentConnections(EntityWithPath entity) {
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
    public Collection<EntityWithPathConnection> getChildConnections(EntityWithPath entity) {
        return entity.getChildConnections();
    }

    @Override
    public void disconnectAllParents(URI nodeId) {
        var node = nodeRepository.findFirstByPublicIdIncludingCachedUrlsAndTranslations(nodeId).orElseThrow(() -> new NotFoundHttpResponseException("Node was not found"));
        node.getParentConnections()
                .forEach(connection -> disconnectParentChildConnection((NodeConnection) connection));
    }

    @Override
    public void disconnectAllChildren(EntityWithPath entity) {
        Set.copyOf(entity.getChildConnections()).forEach(connection -> {
            if (connection instanceof NodeConnection) {
                disconnectParentChildConnection((NodeConnection) connection);
            } else {
                throw new IllegalStateException("Unknown child object on entity trying to disconnect children from");
            }
        });
    }
}
