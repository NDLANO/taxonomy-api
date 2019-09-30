package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.repositories.SubjectTopicRepository;
import no.ndla.taxonomy.repositories.TopicResourceRepository;
import no.ndla.taxonomy.repositories.TopicSubtopicRepository;
import no.ndla.taxonomy.service.exceptions.DuplicateConnectionException;
import no.ndla.taxonomy.service.exceptions.InvalidArgumentServiceException;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.logging.Logger;

@Transactional
@Service
public class EntityConnectionServiceImpl implements EntityConnectionService {
    private SubjectTopicRepository subjectTopicRepository;
    private TopicSubtopicRepository topicSubtopicRepository;
    private TopicResourceRepository topicResourceRepository;


    public EntityConnectionServiceImpl(SubjectTopicRepository subjectTopicRepository, TopicSubtopicRepository topicSubtopicRepository, TopicResourceRepository topicResourceRepository) {
        this.subjectTopicRepository = subjectTopicRepository;
        this.topicSubtopicRepository = topicSubtopicRepository;
        this.topicResourceRepository = topicResourceRepository;
    }

    @Override
    public SubjectTopic connectSubjectTopic(Subject subject, Topic topic) throws DuplicateConnectionException, InvalidArgumentServiceException {
        final var highestRank = subject.getSubjectTopics().stream()
                .map(SubjectTopic::getRank)
                .max(Integer::compare);

        return connectSubjectTopic(subject, topic, highestRank.orElse(0) + 1);
    }

    @Override
    public TopicSubtopic connectTopicSubtopic(Topic topic, Topic subTopic) throws DuplicateConnectionException, InvalidArgumentServiceException {
        return connectTopicSubtopic(topic, subTopic, null);
    }

    @Override
    public TopicResource connectTopicResource(Topic topic, Resource resource) throws InvalidArgumentServiceException, DuplicateConnectionException {
        return connectTopicResource(topic, resource, null);
    }

    private EntityWithPathConnection doCreateConnection(EntityWithPath parent, EntityWithPath child, int rank) {
        EntityWithPathConnection connection;

        if (parent instanceof Subject && child instanceof Topic) {
            connection = SubjectTopic.create((Subject) parent, (Topic) child);
        } else if (parent instanceof Topic && child instanceof Topic) {
            connection = TopicSubtopic.create((Topic) parent, (Topic) child);
        } else if (parent instanceof Topic && child instanceof Resource) {
            connection = TopicResource.create((Topic) parent, (Resource) child);
        } else {
            throw new IllegalArgumentException("Unknown parent-child connection");
        }

        updateRank(connection, rank);

        return connection;
    }

    private SubjectTopic createConnection(Subject subject, Topic topic, int rank) {
        return (SubjectTopic) doCreateConnection(subject, topic, rank);
    }

    private TopicSubtopic createConnection(Topic topic, Topic subtopic, int rank) {
        return (TopicSubtopic) doCreateConnection(topic, subtopic, rank);
    }

    private TopicResource createConnection(Topic topic, Resource resource, int rank) {
        return (TopicResource) doCreateConnection(topic, resource, rank);
    }

    @Override
    public SubjectTopic connectSubjectTopic(Subject subject, Topic topic, Integer rank) throws DuplicateConnectionException {
        if (topic.getParentConnection().isPresent()) {
            throw new DuplicateConnectionException();
        }

        if (rank == null) {
            rank = topic.getSubjectTopics().stream()
                    .map(SubjectTopic::getRank)
                    .max(Integer::compare)
                    .orElse(0) + 1;
        }

        return subjectTopicRepository.saveAndFlush(createConnection(subject, topic, rank));
    }

    @Override
    public TopicSubtopic connectTopicSubtopic(Topic topic, Topic subTopic, Integer rank) throws DuplicateConnectionException, InvalidArgumentServiceException {
        if (subTopic.getParentConnection().isPresent()) {
            throw new DuplicateConnectionException();
        }

        if (topic == subTopic) {
            throw new InvalidArgumentServiceException("Cannot connect topic to itself");
        }

        EntityWithPath parentConnected = topic;

        var ttl = 100;
        while (parentConnected.getParentConnection().map(EntityWithPathConnection::getConnectedParent).isPresent()) {
            Logger.getLogger(this.getClass().toString()).info(parentConnected.getPublicId().toString());
            parentConnected = parentConnected.getParentConnection().orElseThrow().getConnectedParent().orElseThrow();

            if (ttl-- < 0) {
                throw new InvalidArgumentServiceException("Too many levels to get top level object");
            }
            if (parentConnected == subTopic) {
                throw new InvalidArgumentServiceException("Loop detected when trying to connect");
            }
        }

        if (rank == null) {
            rank = topic.getChildrenTopicSubtopics().stream()
                    .map(TopicSubtopic::getRank)
                    .max(Integer::compare)
                    .orElse(0) + 1;
        }

        return topicSubtopicRepository.saveAndFlush(createConnection(topic, subTopic, rank));
    }

    @Override
    public TopicResource connectTopicResource(Topic topic, Resource resource, Integer rank) throws DuplicateConnectionException, InvalidArgumentServiceException {
        if (resource.getParentConnection().isPresent()) {
            throw new DuplicateConnectionException();
        }

        if (rank == null) {
            rank = topic.getTopicResources().stream()
                    .map(TopicResource::getRank)
                    .max(Integer::compare)
                    .orElse(0) + 1;
        }

        return topicResourceRepository.saveAndFlush(createConnection(topic, resource, rank));
    }

    @Override
    public void disconnectTopicSubtopic(Topic topic, Topic subTopic) {
        new HashSet<>(topic.getChildrenTopicSubtopics()).stream()
                .filter(topicSubtopic -> topicSubtopic.getSubtopic().orElse(null) == subTopic)
                .forEach(this::disconnectTopicSubtopic); // (It will never be more than one record)
    }

    @Override
    public void disconnectTopicSubtopic(TopicSubtopic topicSubtopic) {
        topicSubtopic.disassociate();
        topicSubtopicRepository.delete(topicSubtopic);

        topicSubtopicRepository.flush();
    }

    @Override
    public void disconnectSubjectTopic(Subject subject, Topic topic) {
        new HashSet<>(subject.getSubjectTopics()).stream()
                .filter(subjectTopic -> subjectTopic.getTopic().orElse(null) == topic)
                .forEach(this::disconnectSubjectTopic); // (It will never be more than one record)
    }

    @Override
    public void disconnectSubjectTopic(SubjectTopic subjectTopic) {
        subjectTopic.disassociate();
        subjectTopicRepository.delete(subjectTopic);
    }

    @Override
    public void disconnectTopicResource(Topic topic, Resource resource) {
        new HashSet<>(topic.getTopicResources()).stream()
                .filter(topicResource -> topicResource.getResource().orElse(null) == resource)
                .forEach(this::disconnectTopicResource); // (It will never be more than one record)
    }

    @Override
    public void disconnectTopicResource(TopicResource topicResource) {
        topicResource.disassociate();
        topicResourceRepository.delete(topicResource);

        topicResourceRepository.flush();
    }

    private void saveConnections(Collection<EntityWithPathConnection> connections) {
        connections.forEach(connectable -> {
            if (connectable instanceof SubjectTopic) {
                subjectTopicRepository.save((SubjectTopic) connectable);
            } else if (connectable instanceof TopicSubtopic) {
                topicSubtopicRepository.save((TopicSubtopic) connectable);
            } else if (connectable instanceof TopicResource) {
                topicResourceRepository.save((TopicResource) connectable);
            } else {
                throw new IllegalArgumentException("Unknown instance of PrimaryPathConnectable: " + connectable.getClass().toString());
            }
        });

        subjectTopicRepository.flush();
        topicSubtopicRepository.flush();
        topicResourceRepository.flush();
    }

    private void updateRank(EntityWithPathConnection rankable, int newRank) {
        final var updatedConnections = RankableConnectionUpdater.rank(new ArrayList<>(rankable.getConnectedParent().orElseThrow(() -> new IllegalStateException("Rankable parent not found")).getChildConnections()), rankable, newRank);
        saveConnections(updatedConnections);
    }

    @Override
    public void updateTopicSubtopic(TopicSubtopic topicSubtopic, Integer newRank) throws InvalidArgumentServiceException, NotFoundServiceException {
        updateRank(topicSubtopic, newRank);
    }

    @Override
    public void updateTopicResource(TopicResource topicResource, Integer newRank) throws InvalidArgumentServiceException, NotFoundServiceException {
        updateRank(topicResource, newRank);
    }

    @Override
    public void updateSubjectTopic(SubjectTopic subjectTopic, Integer newRank) throws InvalidArgumentServiceException, NotFoundServiceException {
        updateRank(subjectTopic, newRank);
    }

    @Override
    public Optional<EntityWithPathConnection> getParentConnection(EntityWithPath entity) {
        return entity.getParentConnection();
    }

    @Override
    public Collection<EntityWithPathConnection> getChildConnections(EntityWithPath entity) {
        return entity.getChildConnections();
    }
}
