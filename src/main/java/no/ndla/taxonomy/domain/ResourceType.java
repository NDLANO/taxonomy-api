package no.ndla.taxonomy.domain;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import java.net.URI;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Entity
public class ResourceType extends DomainObject {

    public ResourceType() {
        setPublicId(URI.create("urn:resourcetype:" + UUID.randomUUID()));
    }

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private ResourceType parent;

    @OneToMany(mappedBy = "parent")
    private Set<ResourceType> subtypes = new HashSet<>();

    @OneToMany(mappedBy = "resourceType")
    private Set<ResourceTypeTranslation> resourceTypeTranslations = new HashSet<>();

    @OneToMany(mappedBy = "resourceType")
    private Set<TopicResourceType> topicResourceTypes = new HashSet<>();

    @OneToMany(mappedBy = "resourceResourceType")
    private Set<ResourceResourceType> resourceResourceTypes = new HashSet<>();

    public Set<TopicResourceType> getTopicResourceTypes() {
        return topicResourceTypes;
    }

    public void addTopicResourceType(TopicResourceType topicResourceType) {
        this.topicResourceTypes.add(topicResourceType);

        if (topicResourceType.getResourceType() != this) {
            topicResourceType.setResourceType(this);
        }
    }

    public ResourceType name(String name) {
        setName(name);
        return this;
    }

    public Optional<ResourceType> getParent() {
        return Optional.ofNullable(parent);
    }

    public void setParent(ResourceType parent) {
        this.parent = parent;
        if (parent != null) {
            parent.addSubtype(this);
        }
    }

    public ResourceType parent(ResourceType parent) {
        setParent(parent);
        return this;
    }

    public void addSubtype(ResourceType subtype) {
        subtypes.add(subtype);
    }

    public Set<ResourceType> getSubtypes() {
        return this.subtypes;
    }

    public ResourceTypeTranslation addTranslation(String languageCode) {
        ResourceTypeTranslation resourceTypeTranslation = getTranslation(languageCode).orElse(null);
        if (resourceTypeTranslation != null) return resourceTypeTranslation;

        resourceTypeTranslation = new ResourceTypeTranslation(this, languageCode);
        resourceTypeTranslations.add(resourceTypeTranslation);
        return resourceTypeTranslation;
    }

    public Optional<ResourceTypeTranslation> getTranslation(String languageCode) {
        return resourceTypeTranslations.stream()
                .filter(resourceTypeTranslation -> resourceTypeTranslation.getLanguageCode().equals(languageCode))
                .findFirst();
    }

    public Set<ResourceTypeTranslation> getTranslations() {
        return resourceTypeTranslations;
    }

    public void removeTranslation(String languageCode) {
        getTranslation(languageCode).ifPresent(this::removeTranslation);
    }

    public void addTranslation(ResourceTypeTranslation resourceTypeTranslation) {
        this.resourceTypeTranslations.add(resourceTypeTranslation);
        if (resourceTypeTranslation.getResourceType() != this) {
            resourceTypeTranslation.setResourceType(this);
        }
    }

    public void removeTranslation(ResourceTypeTranslation resourceTypeTranslation) {
        if (resourceTypeTranslation.getResourceType() == this) {
            resourceTypeTranslations.remove(resourceTypeTranslation);
            if (resourceTypeTranslation.getResourceType() == this) {
                resourceTypeTranslation.setResourceType(null);
            }
        }
    }

    public Set<ResourceResourceType> getResourceResourceTypes() {
        return this.resourceResourceTypes;
    }

    public void addResourceResourceType(ResourceResourceType resourceResourceType) {
        this.resourceResourceTypes.add(resourceResourceType);

        if (resourceResourceType.getResourceType() != this) {
            resourceResourceType.setResourceType(this);
        }
    }

    public void removeResourceResourceType(ResourceResourceType resourceResourceType) {
        this.resourceResourceTypes.remove(resourceResourceType);

        if (resourceResourceType.getResourceType() == this) {
            resourceResourceType.setResourceType(null);
        }
    }
}
