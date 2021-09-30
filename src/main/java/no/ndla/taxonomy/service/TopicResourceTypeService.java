/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.ResourceType;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.domain.TopicResourceType;
import no.ndla.taxonomy.repositories.ResourceTypeRepository;
import no.ndla.taxonomy.repositories.TopicRepository;
import no.ndla.taxonomy.repositories.TopicResourceTypeRepository;
import no.ndla.taxonomy.service.exceptions.InvalidArgumentServiceException;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class TopicResourceTypeService {
    private final TopicRepository topicRepository;
    private final ResourceTypeRepository resourceTypeRepository;
    private final TopicResourceTypeRepository topicResourceTypeRepository;

    public TopicResourceTypeService(TopicRepository topicRepository, ResourceTypeRepository resourceTypeRepository, TopicResourceTypeRepository topicResourceTypeRepository) {
        this.topicRepository = topicRepository;
        this.resourceTypeRepository = resourceTypeRepository;
        this.topicResourceTypeRepository = topicResourceTypeRepository;
    }

    @Transactional
    public List<TopicResourceType> getTopicResourceTypes(URI topicId) {
        if (topicId == null) {
            throw new InvalidArgumentServiceException(new NullPointerException("Topic ID is null"));
        }
        Topic topic = topicRepository.findByPublicId(topicId);
        if (topic == null) {
            throw new NotFoundServiceException("Topic not found " + topicId.toString());
        }
        return topicResourceTypeRepository.findAllByTopic(topic);
    }

    public URI addTopicResourceType(URI topicId, URI resourceTypeId) {
        if (topicId == null || resourceTypeId == null) {
            throw new InvalidArgumentServiceException(new NullPointerException("Topic or resource type ID is null"));
        }
        Topic topic = topicRepository.findByPublicId(topicId);
        if (topic == null) {
            throw new NotFoundServiceException("Topic not found " + topicId.toString());
        }
        ResourceType resourceType = resourceTypeRepository.findByPublicId(resourceTypeId);
        if (resourceType == null) {
            throw new NotFoundServiceException("Resource type not found " + resourceTypeId.toString());
        }
        TopicResourceType topicResourceType = topic.addResourceType(resourceType);
        topicRepository.save(topic);
        return topicResourceType.getPublicId();
    }

    public void deleteTopicResourceType(URI connectionId) {
        if (connectionId == null) {
            throw new InvalidArgumentServiceException(new NullPointerException("Connection ID is null"));
        }
        TopicResourceType topicResourceType = topicResourceTypeRepository.findByPublicId(connectionId);
        if (topicResourceType == null) {
            throw new NotFoundServiceException("TopicResourceType link object not found: " + connectionId.toString());
        }
        topicResourceTypeRepository.delete(topicResourceType);
    }

    public Stream<TopicResourceType> findAll() {
        return topicResourceTypeRepository.findAll().stream();
    }

    public Optional<TopicResourceType> findById(URI id) {
        return Optional.ofNullable(topicResourceTypeRepository.findByPublicId(id));
    }
}
