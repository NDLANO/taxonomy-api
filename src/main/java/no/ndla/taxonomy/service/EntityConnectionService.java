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
    SubjectTopic connectSubjectTopic(Subject subject, Topic topic, Relevance relevance);

    TopicSubtopic connectTopicSubtopic(Topic topic, Topic subTopic, Relevance relevance);

    TopicSubtopic connectTopicSubtopic(Topic topic, Topic subTopic, Relevance relevance, Integer rank);

    SubjectTopic connectSubjectTopic(Subject subject, Topic topic, Relevance relevance, Integer rank);

    TopicResource connectTopicResource(Topic topic, Resource resource, Relevance relevance);

    TopicResource connectTopicResource(Topic topic, Resource resource, Relevance relevance, boolean isPrimary,
            Integer rank);

    NodeConnection connectParentChild(Node parent, Node child, Relevance relevance, Integer rank);

    NodeResource connectNodeResource(Node node, Resource resource, Relevance relevance, boolean isPrimary,
            Integer rank);

    void disconnectTopicSubtopic(Topic topic, Topic subTopic);

    void disconnectTopicSubtopic(TopicSubtopic topicSubtopic);

    void disconnectSubjectTopic(Subject subject, Topic topic);

    void disconnectSubjectTopic(SubjectTopic subjectTopic);

    void disconnectTopicResource(Topic topic, Resource resource);

    void disconnectTopicResource(TopicResource topicResource);

    void disconnectParentChild(Node parent, Node child);

    void disconnectParentChildConnection(NodeConnection nodeConnection);

    void disconnectNodeResource(Node node, Resource resource);

    void disconnectNodeResource(NodeResource topicResource);

    void updateTopicSubtopic(TopicSubtopic topicSubtopic, Relevance relevance, Integer newRank);

    void updateTopicResource(TopicResource topicResource, Relevance relevance, boolean isPrimary, Integer newRank);

    void updateNodeResource(NodeResource topicResource, Relevance relevance, boolean isPrimary, Integer newRank);

    void updateSubjectTopic(SubjectTopic subjectTopic, Relevance relevance, Integer newRank);

    void updateParentChild(NodeConnection nodeConnection, Relevance relevance, Integer newRank);

    void replacePrimaryConnectionsFor(EntityWithPath entity);

    Collection<EntityWithPathConnection> getParentConnections(EntityWithPath entity);

    Collection<EntityWithPathConnection> getChildConnections(EntityWithPath entity);

    void disconnectAllChildren(EntityWithPath entity);
}
