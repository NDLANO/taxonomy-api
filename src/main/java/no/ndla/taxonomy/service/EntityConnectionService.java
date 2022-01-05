/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.*;

import java.util.Collection;

public interface EntityConnectionService {
    NodeConnection connectParentChild(Node parent, Node child, Relevance relevance, Integer rank);

    NodeResource connectNodeResource(Node node, Resource resource, Relevance relevance, boolean isPrimary,
            Integer rank);

    void disconnectParentChild(Node parent, Node child);

    void disconnectParentChildConnection(NodeConnection nodeConnection);

    void disconnectNodeResource(Node node, Resource resource);

    void disconnectNodeResource(NodeResource topicResource);

    void updateNodeResource(NodeResource topicResource, Relevance relevance, boolean isPrimary, Integer newRank);

    void updateParentChild(NodeConnection nodeConnection, Relevance relevance, Integer newRank);

    void replacePrimaryConnectionsFor(EntityWithPath entity);

    Collection<EntityWithPathConnection> getParentConnections(EntityWithPath entity);

    Collection<EntityWithPathConnection> getChildConnections(EntityWithPath entity);

    void disconnectAllChildren(EntityWithPath entity);
}
