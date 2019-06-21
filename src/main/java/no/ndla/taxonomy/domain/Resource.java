package no.ndla.taxonomy.domain;

import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.net.URI;
import java.util.*;

@Entity
public class Resource extends CachedUrlEntity {

    @Column
    @Type(type = "no.ndla.taxonomy.hibernate.UriType")
    private URI contentUri;

    @OneToMany(mappedBy = "resource", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ResourceResourceType> resourceResourceTypes = new HashSet<>();

    @OneToMany(mappedBy = "resource", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ResourceTranslation> resourceTranslations = new HashSet<>();

    @OneToMany(mappedBy = "resource", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TopicResource> topics = new HashSet<>();

    @OneToMany(mappedBy = "resource", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ResourceFilter> filters = new HashSet<>();

    public Resource() {
        setPublicId(URI.create("urn:resource:" + UUID.randomUUID()));
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
        ResourceTranslation resourceTranslation = getTranslation(languageCode).orElse(null);
        if (resourceTranslation != null) return resourceTranslation;

        resourceTranslation = new ResourceTranslation(this, languageCode);
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
        final var resourceFilter = new ResourceFilter(this, filter, relevance);
        addResourceFilter(resourceFilter);
        return resourceFilter;
    }

    public void removeFilter(Filter filter) {
        ResourceFilter resourceFilter = getFilter(filter);
        if (filter == null) {
            throw new ChildNotFoundException("Resource with id " + this.getPublicId() + " does not have resource-filter " + resourceFilter.getPublicId());
        }

        removeResourceFilter(resourceFilter);
    }

    private ResourceFilter getFilter(Filter filter) {
        for (ResourceFilter rf : filters) {
            if (rf.getFilter().equals(filter)) return rf;
        }
        return null;
    }

    @PreRemove
    void preRemove() {
        for (var topicResource : this.getTopicResources().toArray()) {
            this.removeTopicResource((TopicResource) topicResource);
        }

        for (var resourceFilter : this.getResourceFilters().toArray()) {
            this.removeResourceFilter((ResourceFilter) resourceFilter);
        }
    }

    public Set<ResourceResourceType> getResourceResourceTypes() {
        return this.resourceResourceTypes;
    }

    public Set<ResourceFilter> getResourceFilters() {
        return this.filters;
    }

    public Set<TopicResource> getTopicResources() {
        return this.topics;
    }

    public void removeTopicResource(TopicResource topicResource) {
        this.topics.remove(topicResource);

        if (topicResource.getResource() == this) {
            topicResource.setResource(null);
        }
    }

    public void addResourceFilter(ResourceFilter resourceFilter) {
        this.filters.add(resourceFilter);

        if (resourceFilter.getResource() != this) {
            resourceFilter.setResource(this);
        }
    }

    public void removeResourceFilter(ResourceFilter resourceFilter) {
        this.filters.remove(resourceFilter);

        if (resourceFilter.getResource() == this) {
            resourceFilter.setResource(null);
        }
    }
}