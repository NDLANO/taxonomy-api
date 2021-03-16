package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.*;

import java.util.Collection;

public interface EntityConnectionService {
    TopicSubtopic connectSubjectTopic(Topic subject, Topic topic, Relevance relevance);

    TopicSubtopic connectTopicSubtopic(Topic topic, Topic subTopic, Relevance relevance);

    TopicSubtopic connectTopicSubtopic(Topic topic, Topic subTopic, Relevance relevance, Integer rank);

    TopicSubtopic connectSubjectTopic(Topic subject, Topic topic, Relevance relevance, Integer rank);

    TopicResource connectTopicResource(Topic topic, Resource resource, Relevance relevance);

    TopicResource connectTopicResource(Topic topic, Resource resource, Relevance relevance, boolean isPrimary, Integer rank);

    void disconnectTopicSubtopic(Topic topic, Topic subTopic);

    void disconnectTopicSubtopic(TopicSubtopic topicSubtopic);

    void disconnectTopicResource(Topic topic, Resource resource);

    void disconnectTopicResource(TopicResource topicResource);

    void updateTopicSubtopic(TopicSubtopic topicSubtopic, Relevance relevance, Integer newRank);

    void updateTopicResource(TopicResource topicResource, Relevance relevance, boolean isPrimary, Integer newRank);

    void updateSubjectTopic(TopicSubtopic subjectTopic, Relevance relevance, Integer newRank);

    void replacePrimaryConnectionsFor(EntityWithPath entity);

    Collection<EntityWithPathConnection> getParentConnections(EntityWithPath entity);

    Collection<EntityWithPathConnection> getChildConnections(EntityWithPath entity);

    void disconnectAllChildren(EntityWithPath entity);
}
