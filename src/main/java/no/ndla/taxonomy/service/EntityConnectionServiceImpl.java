/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.repositories.NodeConnectionRepository;
import no.ndla.taxonomy.repositories.NodeResourceRepository;
import no.ndla.taxonomy.service.exceptions.DuplicateConnectionException;
import no.ndla.taxonomy.service.exceptions.InvalidArgumentServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

@Transactional(propagation = Propagation.MANDATORY)
@Service
public class EntityConnectionServiceImpl implements EntityConnectionService {
    private final NodeConnectionRepository nodeConnectionRepository;
    private final NodeResourceRepository nodeResourceRepository;

    private final CachedUrlUpdaterService cachedUrlUpdaterService;

    public EntityConnectionServiceImpl(NodeConnectionRepository nodeConnectionRepository,
            NodeResourceRepository nodeResourceRepository, CachedUrlUpdaterService cachedUrlUpdaterService) {
        this.nodeConnectionRepository = nodeConnectionRepository;
        this.nodeResourceRepository = nodeResourceRepository;
        this.cachedUrlUpdaterService = cachedUrlUpdaterService;
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
        } else if (parent instanceof Node && child instanceof Resource) {
            connection = NodeResource.create((Node) parent, (Resource) child);
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

    private NodeConnection createConnection(Node parent, Node child, Relevance relevance, int rank) {
        return (NodeConnection) doCreateConnection(parent, child, true, relevance, rank);
    }

    private NodeResource createConnection(Node node, Resource resource, Relevance relevance, boolean primary,
            int rank) {
        return (NodeResource) doCreateConnection(node, resource, primary, relevance, rank);
    }

    @Override
    public NodeConnection connectParentChild(Node parent, Node child, Relevance relevance, Integer rank) {
        if (child.getParentConnections().size() > 0) {
            throw new DuplicateConnectionException();
        }

        if (parent == child) {
            throw new InvalidArgumentServiceException("Cannot connect node to itself");
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
            rank = parent.getChildConnections().stream().map(EntityWithPathConnection::getRank).max(Integer::compare)
                    .orElse(0) + 1;
        }

        return nodeConnectionRepository.saveAndFlush(createConnection(parent, child, relevance, rank));
    }

    @Override
    public NodeResource connectNodeResource(Node node, Resource resource, Relevance relevance, boolean isPrimary,
            Integer rank) {
        if (node.getNodeResources().stream()
                .anyMatch(nodeResource -> nodeResource.getResource().orElse(null) == resource)) {
            throw new DuplicateConnectionException();
        }

        if (rank == null) {
            rank = node.getNodeResources().stream().map(NodeResource::getRank).max(Integer::compare).orElse(0) + 1;
        }

        return nodeResourceRepository.saveAndFlush(createConnection(node, resource, relevance, isPrimary, rank));
    }

    @Override
    public void disconnectParentChild(Node parent, Node child) {
        new HashSet<>(parent.getChildConnections()).stream()
                .filter(connection -> connection.getConnectedChild().orElse(null) == child)
                .forEach(connection -> disconnectParentChildConnection((NodeConnection) connection)); // (It will never
                                                                                                      // be more than
                                                                                                      // one record)
    }

    @Override
    public void disconnectParentChildConnection(NodeConnection nodeConnection) {
        final var child = nodeConnection.getChild();

        nodeConnection.disassociate();
        nodeConnectionRepository.delete(nodeConnection);

        child.ifPresent(cachedUrlUpdaterService::updateCachedUrls);

        nodeConnectionRepository.flush();
    }

    @Override
    public void disconnectNodeResource(Node node, Resource resource) {
        new HashSet<>(node.getNodeResources()).stream()
                .filter(nodeResource -> nodeResource.getResource().orElse(null) == resource)
                .forEach(this::disconnectNodeResource); // (It will never be more than one record)
    }

    @Override
    public void disconnectNodeResource(NodeResource nodeResource) {
        boolean setNewPrimary = nodeResource.isPrimary().orElse(false) && nodeResource.getResource().isPresent();
        final var resourceOptional = nodeResource.getResource();

        nodeResource.disassociate();
        nodeResourceRepository.delete(nodeResource);

        resourceOptional.ifPresent(resource -> {
            if (setNewPrimary) {
                resource.getNodeResources().stream().findFirst().ifPresent(resource1 -> {
                    resource1.setPrimary(true);
                    nodeResourceRepository.saveAndFlush(resource1);

                    resource1.getResource().ifPresent(cachedUrlUpdaterService::updateCachedUrls);
                });
            }

            cachedUrlUpdaterService.updateCachedUrls(resource);
        });

        nodeResourceRepository.flush();
    }

    private void saveConnections(Collection<EntityWithPathConnection> connections) {
        connections.forEach(connectable -> {
            if (connectable instanceof NodeConnection) {
                nodeConnectionRepository.save((NodeConnection) connectable);
            } else if (connectable instanceof NodeResource) {
                nodeResourceRepository.save((NodeResource) connectable);
            } else {
                throw new IllegalArgumentException(
                        "Unknown instance of PrimaryPathConnectable: " + connectable.getClass().toString());
            }
        });
        nodeConnectionRepository.flush();
        nodeResourceRepository.flush();
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
    public void updateNodeResource(NodeResource nodeResource, Relevance relevance, boolean isPrimary, Integer newRank) {
        updateRankableConnection(nodeResource, isPrimary, newRank);
        updateRelevance(nodeResource, relevance);
    }

    @Override
    public void updateParentChild(NodeConnection nodeConnection, Relevance relevance, Integer newRank) {
        updateRank(nodeConnection, newRank);
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
    public void disconnectAllChildren(EntityWithPath entity) {
        Set.copyOf(entity.getChildConnections()).forEach(connection -> {
            if (connection instanceof NodeConnection) {
                disconnectParentChildConnection((NodeConnection) connection);
            } else if (connection instanceof NodeResource) {
                disconnectNodeResource((NodeResource) connection);
            } else {
                throw new IllegalStateException("Unknown child object on entity trying to disconnect children from");
            }
        });
    }
}
