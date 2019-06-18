package no.ndla.taxonomy.domain;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.*;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Entity
public class Resource extends DomainObject implements ResolvablePathEntity {
    @Id
    private Integer id;

    @UpdateTimestamp
    private Instant updatedAt;

    @Column
    @Type(type = "no.ndla.taxonomy.hibernate.UriType")
    private URI contentUri;

    @OneToMany(mappedBy = "resource", cascade = CascadeType.ALL, orphanRemoval = true)
    public Set<ResourceResourceType> resourceResourceTypes = new HashSet<>();

    @OneToMany(mappedBy = "resource", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ResourceTranslation> resourceTranslations = new HashSet<>();

    @OneToMany(mappedBy = "resource", cascade = CascadeType.ALL, orphanRemoval = true)
    public Set<TopicResource> topics = new HashSet<>();

    @OneToMany(mappedBy = "resource", cascade = CascadeType.ALL, orphanRemoval = true)
    public Set<ResourceFilter> filters = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "publicId", referencedColumnName = "publicId", insertable = false, updatable = false)
    private Set<ResolvedPath> resolvedPaths;

    public Set<ResolvedPath> getResolvedPaths() {
        return resolvedPaths;
    }

    public Optional<ResolvedPath> getPrimaryResolvedPath() {
        return resolvedPaths.stream().filter(ResolvedPath::isPrimary).findFirst();
    }

    public Resource() {
        setPublicId(URI.create("urn:resource:" + UUID.randomUUID()));
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public String getType() {
        return "resource";
    }

    @Override
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    void updateUpdatedAt() {
        this.updatedAt = Instant.now();
    }

    @Override
    @Transactional(readOnly = true)
    public Set<GeneratedPath> generatePaths(final int iterations) {
        return topics
                .stream()
                .flatMap(topicResource -> topicResource.getTopic()
                        .generatePaths(iterations + 1)
                        .stream()
                        .map(generatedPath ->
                                GeneratedPath.builder()
                                        .setParentPath(generatedPath)
                                        .setIsPrimary(topicResource.isPrimary())
                                        .setSubPath(this.getPublicId().getSchemeSpecificPart())
                                        .setParentId(topicResource.getTopic().getPublicId())
                                        .build()
                        )
                )
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional(readOnly = true)
    public Set<GeneratedPath> generatePaths() {
        return generatePaths(0);
    }

    @Override
    public Set<ResolvablePathEntity> getChildren() {
        return Set.of();
    }

    public Resource name(String name) {
        setName(name);
        return this;
    }

    public Iterator<Topic> getTopics() {
        Iterator<TopicResource> iterator = topics.iterator();
        return new Iterator<Topic>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Topic next() {
                return iterator.next().getTopic();
            }
        };
    }

    public Iterator<ResourceType> getResourceTypes() {
        Iterator<ResourceResourceType> iterator = resourceResourceTypes.iterator();
        return new Iterator<ResourceType>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public ResourceType next() {
                return iterator.next().getResourceType();
            }
        };
    }

    public Set<ResourceResourceType> getResourceResourceTypes() {
        return resourceResourceTypes;
    }

    public Set<ResourceFilter> getResourceFilters() {
        return this.filters;
    }

    public Set<TopicResource> getTopicResources() {
        return this.topics;
    }

    public ResourceResourceType addResourceType(ResourceType resourceType) {
        Iterator<ResourceType> resourceTypes = getResourceTypes();
        while (resourceTypes.hasNext()) {
            ResourceType t = resourceTypes.next();
            if (t.getId().equals(resourceType.getId())) {
                throw new DuplicateIdException("Resource with id " + getPublicId() + " is already marked with resource type with id " + resourceType.getPublicId());
            }
        }

        ResourceResourceType resourceResourceType = new ResourceResourceType(this, resourceType);
        resourceResourceTypes.add(resourceResourceType);
        return resourceResourceType;
    }

    public URI getContentUri() {
        return contentUri;
    }

    public void setContentUri(URI contentUri) {
        this.contentUri = contentUri;
    }


    public ResourceTranslation addTranslation(String languageCode) {
        final var existingTranslation = getTranslation(languageCode);
        if (existingTranslation.isPresent()) {
            return existingTranslation.get();
        }

        final var resourceTranslation = new ResourceTranslation(this, languageCode);
        resourceTranslations.add(resourceTranslation);
        return resourceTranslation;
    }

    public Optional<ResourceTranslation> getTranslation(String languageCode) {
        return resourceTranslations.stream()
                .filter(resourceTranslation -> resourceTranslation.getLanguageCode().equals(languageCode))
                .findFirst();
    }

    public Iterator<ResourceTranslation> getTranslations() {
        return resourceTranslations.iterator();
    }

    public void removeTranslation(String languageCode) {
        getTranslation(languageCode).ifPresent(resourceTranslations::remove);
    }

    public Topic getPrimaryTopic() {
        for (TopicResource topic : topics) {
            if (topic.isPrimary()) return topic.getTopic();
        }
        return null;
    }

    public void setPrimaryTopic(Topic topic) {
        TopicResource topicResource = getTopic(topic);
        if (null == topicResource) throw new ParentNotFoundException(this, topic);

        topics.forEach(t -> t.setPrimary(false));
        topicResource.setPrimary(true);
    }

    public void setRandomPrimaryTopic() {
        if (topics.size() == 0) return;
        setPrimaryTopic(topics.iterator().next().getTopic());
    }

    public boolean hasSingleParentTopic() {
        return topics.size() == 1;
    }

    private TopicResource getTopic(Topic topic) {
        for (TopicResource topicResource : topics) {
            if (topicResource.getTopic().equals(topic)) {
                return topicResource;
            }
        }
        return null;
    }

    public void removeResourceType(ResourceType resourceType) {
        ResourceResourceType resourceResourceType = getResourceType(resourceType);
        if (resourceResourceType == null)
            throw new ChildNotFoundException("Resource with id " + this.getPublicId() + " is not of type " + resourceType.getPublicId());

        resourceResourceTypes.remove(resourceResourceType);
    }

    private ResourceResourceType getResourceType(ResourceType resourceType) {
        for (ResourceResourceType resourceResourceType : resourceResourceTypes) {
            if (resourceResourceType.getResourceType().equals(resourceType)) return resourceResourceType;
        }
        return null;
    }

    public ResourceFilter addFilter(Filter filter, Relevance relevance) {
        ResourceFilter resourceFilter = new ResourceFilter(this, filter, relevance);
        filters.add(resourceFilter);
        return resourceFilter;
    }

    public void removeFilter(Filter filter) {
        ResourceFilter resourceFilter = getFilter(filter);
        if (filter == null) {
            throw new ChildNotFoundException("Resource with id " + this.getPublicId() + " does not have resource-filter " + resourceFilter.getPublicId());
        }
        filters.remove(resourceFilter);
    }

    private ResourceFilter getFilter(Filter filter) {
        for (ResourceFilter rf : filters) {
            if (rf.getFilter().equals(filter)) return rf;
        }
        return null;
    }
}