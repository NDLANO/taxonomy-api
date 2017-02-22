package no.ndla.taxonomy.service.domain;


import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

@Entity
public class Resource extends DomainObject {

    @Column
    @Type(type = "no.ndla.taxonomy.service.hibernate.UriType")
    private URI contentUri;

    @OneToMany(mappedBy = "resource")
    private Set<ResourceResourceType> resourceResourceTypes = new HashSet<>();

    @OneToMany(mappedBy = "resource")
    Set<ResourceTranslation> resourceTranslations = new HashSet<>();

    @OneToMany(mappedBy = "resource")
    private Set<TopicResource> topicResources = new HashSet<>();

    public Resource() {
        setPublicId(URI.create("urn:resource:" + UUID.randomUUID()));
    }

    public Resource name(String name) {
        setName(name);
        return this;
    }


    public Iterator<TopicResource> getTopicResources() {
        Iterator<TopicResource> iterator = topicResources.iterator();
        return new Iterator<TopicResource>() {
            @Override
            public boolean hasNext() {return iterator.hasNext(); }

            @Override
            public TopicResource next() { return iterator.next(); }
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
        ResourceTranslation resourceTranslation = getTranslation(languageCode);
        if (resourceTranslation != null) return resourceTranslation;

        resourceTranslation = new ResourceTranslation(this, languageCode);
        resourceTranslations.add(resourceTranslation);
        return resourceTranslation;
    }

    public ResourceTranslation getTranslation(String languageCode) {
        return resourceTranslations.stream()
                .filter(resourceTranslation -> resourceTranslation.getLanguageCode().equals(languageCode))
                .findFirst()
                .orElse(null);
    }

    public Iterator<ResourceTranslation> getTranslations() {
        return resourceTranslations.iterator();
    }

    public void removeTranslation(String languageCode) {
        ResourceTranslation translation = getTranslation(languageCode);
        if (translation == null) return;
        resourceTranslations.remove(translation);
    }

    public void addTopicResource(TopicResource topicResource, boolean primary) {
        topicResources.add(topicResource);
        setPrimary(primary, topicResource);
    }


    private void setPrimary(boolean primary, TopicResource topicResource) {
        Topic topic = topicResource.getTopic();
        if (isFirstTopicForResource(topic)) {
            topicResource.setPrimary(true);
        } else {
            topicResource.setPrimary(primary);
            if (primary) {
                inValidateOldPrimary(topicResource);
            }
        }
    }


    private boolean isFirstTopicForResource(Topic topic) {
        if (topicResources.size() > 1) {
            return false;
        }
        TopicResource topicResource = getTopicResources().next();
        return topicResource.getResource().getPublicId().equals(this.getPublicId()) && topicResource.getTopic().getPublicId().equals(topic.getPublicId());
    }

    private void inValidateOldPrimary(TopicResource topicResource) {
        for (TopicResource tr: topicResources) {
            if (!tr.getPublicId().equals(topicResource.getPublicId())) {
                tr.setPrimary(false);
            }
        }
    }
}