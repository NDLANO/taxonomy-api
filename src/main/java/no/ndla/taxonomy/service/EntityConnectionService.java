package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.*;

import java.util.Collection;

public interface EntityConnectionService {
    SubjectTopic connectSubjectTopic(Subject subject, Topic topic);

    TopicSubtopic connectTopicSubtopic(Topic topic, Topic subTopic);

    TopicSubtopic connectTopicSubtopic(Topic topic, Topic subTopic, Integer rank);

    SubjectTopic connectSubjectTopic(Subject subject, Topic topic, Integer rank);

    TopicResource connectTopicResource(Topic topic, Resource resource);

    TopicResource connectTopicResource(Topic topic, Resource resource, boolean isPrimary, Integer rank);

    void disconnectTopicSubtopic(Topic topic, Topic subTopic);

    void disconnectTopicSubtopic(TopicSubtopic topicSubtopic);

    void disconnectSubjectTopic(Subject subject, Topic topic);

    void disconnectSubjectTopic(SubjectTopic subjectTopic);

    void disconnectTopicResource(Topic topic, Resource resource);

    void disconnectTopicResource(TopicResource topicResource);

    void updateTopicSubtopic(TopicSubtopic topicSubtopic, Integer newRank);

    void updateTopicResource(TopicResource topicResource, boolean isPrimary, Integer newRank);

    void updateSubjectTopic(SubjectTopic subjectTopic, Integer newRank);

    void replacePrimaryConnectionsFor(EntityWithPath entity);

    Collection<EntityWithPathConnection> getParentConnections(EntityWithPath entity);

    Collection<EntityWithPathConnection> getChildConnections(EntityWithPath entity);
}
