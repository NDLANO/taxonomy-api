package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.repositories.TopicResourceRepository;
import no.ndla.taxonomy.repositories.TopicSubtopicRepository;
import no.ndla.taxonomy.service.exceptions.DuplicateConnectionException;
import no.ndla.taxonomy.service.exceptions.InvalidArgumentServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Transactional(propagation = Propagation.MANDATORY)
@Service
public class EntityConnectionServiceImpl implements EntityConnectionService {
    private final TopicSubtopicRepository topicSubtopicRepository;
    private final TopicResourceRepository topicResourceRepository;

    private final CachedUrlUpdaterService cachedUrlUpdaterService;


    public EntityConnectionServiceImpl(TopicSubtopicRepository topicSubtopicRepository, TopicResourceRepository topicResourceRepository, CachedUrlUpdaterService cachedUrlUpdaterService) {
        this.topicSubtopicRepository = topicSubtopicRepository;
        this.topicResourceRepository = topicResourceRepository;
        this.cachedUrlUpdaterService = cachedUrlUpdaterService;
    }

    @Override
    public TopicSubtopic connectSubjectTopic(Topic subject, Topic topic, Relevance relevance) {
        final var highestRank = subject.getChildrenTopicSubtopics().stream()
                .map(TopicSubtopic::getRank)
                .max(Integer::compare);

        return connectSubjectTopic(subject, topic, relevance, highestRank.orElse(0) + 1);
    }

    @Override
    public TopicResource connectTopicResource(Topic topic, Resource resource, Relevance relevance) {
        return connectTopicResource(topic, resource, relevance, true, null);
    }

    private EntityWithPathConnection doCreateConnection(EntityWithPath parent, EntityWithPath child, boolean requestedPrimary, Relevance relevance, int rank) {
        if (child.getParentConnections().size() == 0) {
            // First connected is always primary regardless of request
            requestedPrimary = true;
        }

        EntityWithPathConnection connection;

        if (parent instanceof Topic && child instanceof Topic) {
            connection = TopicSubtopic.create((Topic) parent, (Topic) child);
        } else if (parent instanceof Topic && child instanceof Resource) {
            connection = TopicResource.create((Topic) parent, (Resource) child);
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

    private TopicSubtopic createConnection(Topic topic, Topic subtopic, Relevance relevance, int rank) {
        return (TopicSubtopic) doCreateConnection(topic, subtopic, true, relevance, rank);
    }

    private TopicResource createConnection(Topic topic, Resource resource, Relevance relevance, boolean primary, int rank) {
        return (TopicResource) doCreateConnection(topic, resource, primary, relevance, rank);
    }

    @Override
    public TopicSubtopic connectSubjectTopic(Topic subject, Topic topic, Relevance relevance, Integer rank) {
        if (topic.getParentConnections().size() > 0) {
            throw new DuplicateConnectionException();
        }

        if (rank == null) {
            rank = subject.getChildrenTopicSubtopics().stream()
                    .map(TopicSubtopic::getRank)
                    .max(Integer::compare)
                    .orElse(0) + 1;
        }

        return topicSubtopicRepository.saveAndFlush(createConnection(subject, topic, relevance, rank));
    }

    @Override
    public TopicSubtopic connectTopicSubtopic(Topic topic, Topic subTopic, Relevance relevance) {
        return connectTopicSubtopic(topic, subTopic, relevance, null);
    }

    @Override
    public TopicSubtopic connectTopicSubtopic(Topic topic, Topic subTopic, Relevance relevance, Integer rank) {
        if (subTopic.getParentConnections().size() > 0) {
            throw new DuplicateConnectionException();
        }

        if (topic == subTopic) {
            throw new InvalidArgumentServiceException("Cannot connect topic to itself");
        }

        EntityWithPath parentConnected = topic;

        var ttl = 100;
        while (parentConnected.getParentConnections().stream().findFirst().map(EntityWithPathConnection::getConnectedParent).isPresent()) {
            Logger.getLogger(this.getClass().toString()).info(parentConnected.getPublicId().toString());
            parentConnected = parentConnected.getParentConnections().stream().findFirst().orElseThrow().getConnectedParent().orElseThrow();

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

        return topicSubtopicRepository.saveAndFlush(createConnection(topic, subTopic, relevance, rank));
    }

    @Override
    public TopicResource connectTopicResource(Topic topic, Resource resource, Relevance relevance, boolean isPrimary, Integer rank) {
        if (topic.getTopicResources().stream()
                .anyMatch(topicResource -> topicResource.getResource().orElse(null) == resource)) {
            throw new DuplicateConnectionException();
        }

        if (rank == null) {
            rank = topic.getTopicResources().stream()
                    .map(TopicResource::getRank)
                    .max(Integer::compare)
                    .orElse(0) + 1;
        }

        return topicResourceRepository.saveAndFlush(createConnection(topic, resource, relevance, isPrimary, rank));
    }

    @Override
    public void disconnectTopicSubtopic(Topic topic, Topic subTopic) {
        new HashSet<>(topic.getChildrenTopicSubtopics()).stream()
                .filter(topicSubtopic -> topicSubtopic.getSubtopic().orElse(null) == subTopic)
                .forEach(this::disconnectTopicSubtopic); // (It will never be more than one record)
    }

    @Override
    public void disconnectTopicSubtopic(TopicSubtopic topicSubtopic) {
        final var subTopic = topicSubtopic.getSubtopic();

        topicSubtopic.disassociate();
        topicSubtopicRepository.delete(topicSubtopic);

        subTopic.ifPresent(cachedUrlUpdaterService::updateCachedUrls);

        topicSubtopicRepository.flush();
    }

    @Override
    public void disconnectTopicResource(Topic topic, Resource resource) {
        new HashSet<>(topic.getTopicResources()).stream()
                .filter(topicResource -> topicResource.getResource().orElse(null) == resource)
                .forEach(this::disconnectTopicResource); // (It will never be more than one record)
    }

    @Override
    public void disconnectTopicResource(TopicResource topicResource) {
        boolean setNewPrimary = topicResource.isPrimary().orElseThrow() && topicResource.getResource().isPresent();
        final var resourceOptional = topicResource.getResource();

        topicResource.disassociate();
        topicResourceRepository.delete(topicResource);

        resourceOptional.ifPresent(resource -> {
            if (setNewPrimary) {
                resource.getTopicResources().stream().findFirst().ifPresent(topicResource1 -> {
                    topicResource1.setPrimary(true);
                    topicResourceRepository.saveAndFlush(topicResource1);

                    topicResource1.getResource().ifPresent(cachedUrlUpdaterService::updateCachedUrls);
                });
            }

            cachedUrlUpdaterService.updateCachedUrls(resource);
        });

        topicResourceRepository.flush();
    }

    private void saveConnections(Collection<EntityWithPathConnection> connections) {
        connections.forEach(connectable -> {
            if (connectable instanceof TopicSubtopic) {
                topicSubtopicRepository.save((TopicSubtopic) connectable);
            } else if (connectable instanceof TopicResource) {
                topicResourceRepository.save((TopicResource) connectable);
            } else {
                throw new IllegalArgumentException("Unknown instance of PrimaryPathConnectable: " + connectable.getClass().toString());
            }
        });

        topicSubtopicRepository.flush();
        topicResourceRepository.flush();
    }

    private void updatePrimaryConnection(EntityWithPathConnection connectable, boolean setPrimaryTo) {
        final var updatedConnectables = new HashSet<EntityWithPathConnection>();
        updatedConnectables.add(connectable);

        // Updates all other nodes connected to this parent
        final var foundNewPrimary = new AtomicBoolean(false);
        connectable.getConnectedChild().ifPresent(entityWithPath -> entityWithPath.getParentConnections()
                .stream()
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

        updatedConnectables.forEach(updatedConnectable -> updatedConnectable.getConnectedChild().ifPresent(cachedUrlUpdaterService::updateCachedUrls));

        if (!setPrimaryTo && !foundNewPrimary.get()) {
            throw new InvalidArgumentServiceException("Requested to set non-primary, but cannot find another node to set primary");
        }
    }

    private void updateRank(EntityWithPathConnection rankable, int newRank) {
        final var updatedConnections = RankableConnectionUpdater.rank(new ArrayList<>(rankable.getConnectedParent().orElseThrow(() -> new IllegalStateException("Rankable parent not found")).getChildConnections()), rankable, newRank);
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
    public void updateTopicSubtopic(TopicSubtopic topicSubtopic, Relevance relevance, Integer newRank) {
        updateRank(topicSubtopic, newRank);
        updateRelevance(topicSubtopic, relevance);
    }

    @Override
    public void updateTopicResource(TopicResource topicResource, Relevance relevance, boolean isPrimary, Integer newRank) {
        updateRankableConnection(topicResource, isPrimary, newRank);
        updateRelevance(topicResource, relevance);
    }

    @Override
    public void updateSubjectTopic(TopicSubtopic subjectTopic, Relevance relevance, Integer newRank) {
        updateRank(subjectTopic, newRank);
        updateRelevance(subjectTopic, relevance);
    }

    @Override
    public void replacePrimaryConnectionsFor(EntityWithPath entity) {
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
    public Collection<EntityWithPathConnection> getParentConnections(EntityWithPath entity) {
        // (applies to both getChildConnections and getParentConnections)
        //
        // While this method will work on any objects implementing the EntityWithPath interface there is an
        // optimized path for Topic objects that will perform better than just reading from the probably
        // lazy initialized properties of the object

        if (entity instanceof Topic) {
            return new ArrayList<>(topicSubtopicRepository
                    .findAllBySubtopicPublicIdIncludingTopicAndSubtopicAndCachedUrls(entity.getPublicId()));
        }

        return entity.getParentConnections();
    }

    @Override
    public Collection<EntityWithPathConnection> getChildConnections(EntityWithPath entity) {
        return entity.getChildConnections();
    }

    @Override
    public void disconnectAllChildren(EntityWithPath entity) {
        Set.copyOf(entity.getChildConnections())
                .forEach(connection -> {
                    if (connection instanceof TopicSubtopic) {
                        disconnectTopicSubtopic((TopicSubtopic) connection);
                    } else if (connection instanceof TopicResource) {
                        disconnectTopicResource((TopicResource) connection);
                    } else {
                        throw new IllegalStateException("Unknown child object on entity trying to disconnect children from");
                    }
                });
    }
}
