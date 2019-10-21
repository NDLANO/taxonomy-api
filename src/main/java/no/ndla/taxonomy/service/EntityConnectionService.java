package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.service.exceptions.DuplicateConnectionException;
import no.ndla.taxonomy.service.exceptions.InvalidArgumentServiceException;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;

import java.util.Collection;
import java.util.Optional;

public interface EntityConnectionService {
    SubjectTopic connectSubjectTopic(Subject subject, Topic topic) throws DuplicateConnectionException, InvalidArgumentServiceException;

    TopicSubtopic connectTopicSubtopic(Topic topic, Topic subTopic) throws DuplicateConnectionException, InvalidArgumentServiceException;

    SubjectTopic connectSubjectTopic(Subject subject, Topic topic, Integer rank) throws DuplicateConnectionException;

    TopicResource connectTopicResource(Topic topic, Resource resource) throws DuplicateConnectionException, InvalidArgumentServiceException;

    TopicSubtopic connectTopicSubtopic(Topic topic, Topic subTopic, Integer rank) throws DuplicateConnectionException, InvalidArgumentServiceException;

    TopicResource connectTopicResource(Topic topic, Resource resource, Integer rank) throws DuplicateConnectionException, InvalidArgumentServiceException;

    void disconnectTopicSubtopic(Topic topic, Topic subTopic);

    void disconnectTopicSubtopic(TopicSubtopic topicSubtopic);

    void disconnectSubjectTopic(Subject subject, Topic topic);

    void disconnectSubjectTopic(SubjectTopic subjectTopic);

    void disconnectTopicResource(Topic topic, Resource resource);

    void disconnectTopicResource(TopicResource topicResource);

    void updateTopicSubtopic(TopicSubtopic topicSubtopic, Integer newRank) throws InvalidArgumentServiceException, NotFoundServiceException;

    void updateTopicResource(TopicResource topicResource, Integer newRank) throws InvalidArgumentServiceException, NotFoundServiceException;

    void updateSubjectTopic(SubjectTopic subjectTopic, Integer newRank) throws InvalidArgumentServiceException, NotFoundServiceException;

    Optional<EntityWithPathConnection> getParentConnection(EntityWithPath entity);

    Collection<EntityWithPathConnection> getChildConnections(EntityWithPath entity);
}
