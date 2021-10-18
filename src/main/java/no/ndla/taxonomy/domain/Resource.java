/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import no.ndla.taxonomy.domain.exceptions.ChildNotFoundException;
import no.ndla.taxonomy.domain.exceptions.DuplicateIdException;

import javax.persistence.*;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@Entity
public class Resource extends EntityWithPath {

    @Column
    private URI contentUri;

    @OneToMany(mappedBy = "resource", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ResourceResourceType> resourceResourceTypes = new HashSet<>();

    @OneToMany(mappedBy = "resource", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ResourceTranslation> resourceTranslations = new HashSet<>();

    @OneToMany(mappedBy = "resource", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TopicResource> topics = new HashSet<>();

    @OneToMany(mappedBy = "resource", cascade = CascadeType.ALL, orphanRemoval = true)
    protected Set<CachedPath> cachedPaths = new HashSet<>();

    @Override
    public Set<CachedPath> getCachedPaths() {
        return cachedPaths;
    }

    @Override
    public Set<EntityWithPathConnection> getParentConnections() {
        return topics.stream().collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<EntityWithPathConnection> getChildConnections() {
        return Set.of();
    }

    public Resource() {
        setPublicId(URI.create("urn:resource:" + UUID.randomUUID()));
    }

    public Collection<Topic> getTopics() {
        return getTopicResources().stream().map(TopicResource::getTopic).map(Optional::get)
                .collect(Collectors.toUnmodifiableSet());
    }

    public Collection<ResourceType> getResourceTypes() {
        return getResourceResourceTypes().stream().map(ResourceResourceType::getResourceType)
                .collect(Collectors.toUnmodifiableSet());
    }

    public ResourceResourceType addResourceType(ResourceType resourceType) {
        if (getResourceTypes().contains(resourceType)) {
            throw new DuplicateIdException("Resource with id " + getPublicId()
                    + " is already marked with resource type with id " + resourceType.getPublicId());
        }

        ResourceResourceType resourceResourceType = ResourceResourceType.create(this, resourceType);
        addResourceResourceType(resourceResourceType);
        return resourceResourceType;
    }

    public void addResourceResourceType(ResourceResourceType resourceResourceType) {
        this.resourceResourceTypes.add(resourceResourceType);

        if (resourceResourceType.getResource() != this) {
            throw new IllegalArgumentException(
                    "ResourceResourceType must have Resource set before being associated with Resource");
        }
    }

    public void removeResourceResourceType(ResourceResourceType resourceResourceType) {
        this.resourceResourceTypes.remove(resourceResourceType);

        if (resourceResourceType.getResource() == this) {
            resourceResourceType.disassociate();
        }
    }

    public URI getContentUri() {
        return contentUri;
    }

    public void setContentUri(URI contentUri) {
        this.contentUri = contentUri;
    }

    public ResourceTranslation addTranslation(String languageCode) {
        ResourceTranslation resourceTranslation = getTranslation(languageCode).orElse(null);
        if (resourceTranslation != null)
            return resourceTranslation;

        resourceTranslation = new ResourceTranslation(this, languageCode);
        resourceTranslations.add(resourceTranslation);
        return resourceTranslation;
    }

    public Optional<ResourceTranslation> getTranslation(String languageCode) {
        return resourceTranslations.stream()
                .filter(resourceTranslation -> resourceTranslation.getLanguageCode().equals(languageCode)).findFirst();
    }

    public Set<ResourceTranslation> getTranslations() {
        return resourceTranslations.stream().collect(Collectors.toUnmodifiableSet());
    }

    public void removeTranslation(String languageCode) {
        getTranslation(languageCode).ifPresent(this::removeTranslation);
    }

    public void addTranslation(ResourceTranslation resourceTranslation) {
        this.resourceTranslations.add(resourceTranslation);
        if (resourceTranslation.getResource() != this) {
            resourceTranslation.setResource(this);
        }
    }

    public void removeTranslation(ResourceTranslation translation) {
        if (translation.getResource() == this) {
            resourceTranslations.remove(translation);
            if (translation.getResource() == this) {
                translation.setResource(null);
            }
        }
    }

    public Optional<Topic> getPrimaryTopic() {
        for (TopicResource topic : topics) {
            if (topic.isPrimary().orElseThrow())
                return topic.getTopic();
        }
        return Optional.empty();
    }

    public void removeResourceType(ResourceType resourceType) {
        ResourceResourceType resourceResourceType = getResourceType(resourceType);
        if (resourceResourceType == null)
            throw new ChildNotFoundException(
                    "Resource with id " + this.getPublicId() + " is not of type " + resourceType.getPublicId());

        resourceResourceTypes.remove(resourceResourceType);
    }

    private ResourceResourceType getResourceType(ResourceType resourceType) {
        for (ResourceResourceType resourceResourceType : resourceResourceTypes) {
            if (resourceResourceType.getResourceType().equals(resourceType))
                return resourceResourceType;
        }
        return null;
    }

    public Set<ResourceResourceType> getResourceResourceTypes() {
        return this.resourceResourceTypes.stream().collect(Collectors.toUnmodifiableSet());
    }

    public Set<TopicResource> getTopicResources() {
        return this.topics.stream().collect(Collectors.toUnmodifiableSet());
    }

    public void removeTopicResource(TopicResource topicResource) {
        this.topics.remove(topicResource);

        if (topicResource.getResource().orElse(null) == this) {
            topicResource.disassociate();
        }
    }

    public void addTopicResource(TopicResource topicResource) {
        this.topics.add(topicResource);

        if (topicResource.getResource().orElse(null) != this) {
            throw new IllegalArgumentException("TopicResource must have Resource relation set before adding");
        }
    }

    @PreRemove
    void preRemove() {
        Set.copyOf(resourceResourceTypes).forEach(ResourceResourceType::disassociate);
        Set.copyOf(topics).forEach(TopicResource::disassociate);
    }

    @Override
    public boolean isContext() {
        return false;
    }
}
