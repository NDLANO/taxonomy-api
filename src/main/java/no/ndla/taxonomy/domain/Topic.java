package no.ndla.taxonomy.domain;


import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.net.URI;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
public class Topic extends CachedUrlEntity {

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SubjectTopic> subjectTopics = new HashSet<>();

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TopicSubtopic> childTopicSubtopics = new HashSet<>();

    @OneToMany(mappedBy = "subtopic", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TopicSubtopic> parentTopicSubtopics = new HashSet<>();

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TopicResource> topicResources = new HashSet<>();

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TopicResourceType> topicResourceTypes = new HashSet<>();

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TopicFilter> topicFilters = new HashSet<>();

    @Column
    @Type(type = "no.ndla.taxonomy.hibernate.UriType")
    private URI contentUri;

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TopicTranslation> translations = new HashSet<>();

    @Column
    private boolean context;

    public Topic() {
        setPublicId(URI.create("urn:topic:" + UUID.randomUUID()));
    }

    public Topic name(String name) {
        setName(name);
        return this;
    }

    /*

        In the old code the primary URL for topics was special since it would try to return
        a context URL (a topic behaving as a subject) rather than a subject URL if that is available.
        Trying to re-implement it by sorting the context URLs first by the same path comparing as the old code

     */
    public Optional<String> getPrimaryPath() {
        return getCachedUrls()
                .stream()
                .filter(CachedUrl::isPrimary)
                .map(CachedUrl::getPath).min((path1, path2) -> {
                    if (path1.startsWith("/topic") && path2.startsWith("/topic")) {
                        return 0;
                    }

                    if (path1.startsWith("/topic")) {
                        return -1;
                    }

                    if (path2.startsWith("/topic")) {
                        return 1;
                    }

                    return 0;
                });
    }

    public Set<SubjectTopic> getSubjectTopics() {
        return this.subjectTopics;
    }

    public void addSubjectTopic(SubjectTopic subjectTopic) {
        this.subjectTopics.add(subjectTopic);

        if (subjectTopic.getTopic().orElse(null) != this) {
            subjectTopic.setTopic(this);
        }
    }

    public void removeSubjectTopic(SubjectTopic subjectTopic) {
        this.subjectTopics.remove(subjectTopic);

        var topic = subjectTopic.getTopic().orElse(null);

        if (topic == this) {
            subjectTopic.setTopic(null);
        }
    }

    public Set<TopicSubtopic> getChildrenTopicSubtopics() {
        return this.childTopicSubtopics;
    }

    public Set<TopicSubtopic> getParentTopicSubtopics() {
        return this.parentTopicSubtopics;
    }

    public void addChildTopicSubtopic(TopicSubtopic topicSubtopic) {
        this.childTopicSubtopics.add(topicSubtopic);

        if (topicSubtopic.getTopic().orElse(null) != this) {
            topicSubtopic.setTopic(this);
            topicSubtopic.setSubtopic(this);
        }
    }

    public void removeChildTopicSubTopic(TopicSubtopic topicSubtopic) {
        this.childTopicSubtopics.remove(topicSubtopic);

        if (topicSubtopic.getTopic().orElse(null) == this) {
            topicSubtopic.setTopic(null);
            topicSubtopic.setSubtopic(null);
        }
    }

    public void addParentTopicSubtopic(TopicSubtopic topicSubtopic) {
        this.parentTopicSubtopics.add(topicSubtopic);

        if (topicSubtopic.getSubtopic().orElse(null) != this) {
            topicSubtopic.setSubtopic(this);
        }
    }

    public void removeParentTopicSubtopic(TopicSubtopic topicSubtopic) {
        this.parentTopicSubtopics.remove(topicSubtopic);

        if (topicSubtopic.getSubtopic().orElse(null) == this) {
            topicSubtopic.setSubtopic(null);
            topicSubtopic.setTopic(null);
        }
    }

    public Set<TopicResource> getTopicResources() {
        return this.topicResources;
    }

    public void addTopicResource(TopicResource topicResource) {
        this.topicResources.add(topicResource);

        if (topicResource.getTopic().orElse(null) != this) {
            topicResource.setTopic(this);
        }
    }

    public void removeTopicResource(TopicResource topicResource) {
        this.topicResources.remove(topicResource);

        if (topicResource.getTopic().orElse(null) == this) {
            topicResource.setTopic(null);
        }
    }

    public Set<TopicResourceType> getTopicResourceTypes() {
        return this.topicResourceTypes;
    }

    public TopicSubtopic addSubtopic(Topic subtopic) {
        return addSubtopic(subtopic, true);
    }

    public TopicSubtopic addSubtopic(Topic subtopic, boolean primaryConnection) {
        refuseIfDuplicateSubtopic(subtopic);

        final var topicSubtopic = new TopicSubtopic();
        topicSubtopic.setPrimary(false);
        topicSubtopic.setTopic(this);
        topicSubtopic.setSubtopic(subtopic);

        if (primaryConnection) {
            subtopic.setPrimaryParentTopic(this);
        }

        return topicSubtopic;
    }

    public void addTopicResourceType(TopicResourceType topicResourceType) {
        this.topicResourceTypes.add(topicResourceType);

        if (topicResourceType.getTopic().orElse(null) != this) {
            topicResourceType.setTopic(this);
        }
    }

    public void removeTopicResourceType(TopicResourceType topicResourceType) {
        this.topicResourceTypes.remove(topicResourceType);

        if (topicResourceType.getTopic().orElse(null) == this) {
            topicResourceType.setTopic(null);
            topicResourceType.setResourceType(null);
        }
    }

    public boolean hasSingleSubject() {
        return subjectTopics.size() == 1;
    }

    private void refuseIfDuplicateSubtopic(Topic subtopic) {
        if (getSubtopics().contains(subtopic)) {
            throw new DuplicateIdException("Topic with id " + getPublicId() + " already contains topic with id " + subtopic.getPublicId());
        }
    }

    public Optional<Topic> getPrimaryParentTopic() {
        for (TopicSubtopic parentTopic : parentTopicSubtopics) {
            if (parentTopic.isPrimary()) return parentTopic.getTopic();
        }
        return Optional.empty();
    }

    public void setPrimaryParentTopic(Topic topic) {
        // Set requested TopicSubtopic object isPrimary to true and all other to false
        parentTopicSubtopics.stream()
                .filter(ts -> ts.getTopic().isPresent())
                .forEach(st -> st.setPrimary(st.getTopic().get().equals(topic)));
    }

    void setRandomPrimaryTopic() {
        parentTopicSubtopics.stream()
                .map(TopicSubtopic::getTopic)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .ifPresent(this::setPrimaryParentTopic);
    }

    public TopicResource addResource(Resource resource) {
        return addResource(resource, true);
    }

    public TopicResource addResource(Resource resource, boolean primaryConnection) {
        refuseIfDuplicateResource(resource);

        TopicResource topicResource = new TopicResource();
        topicResource.setResource(resource);
        addTopicResource(topicResource);

        if (primaryConnection) {
            resource.setPrimaryTopic(this);
        } else {
            topicResource.setPrimary(false);
        }

        return topicResource;
    }

    private void refuseIfDuplicateResource(Resource resource) {
        if (getResources().contains(resource)) {
            throw new DuplicateIdException("Topic with id " + getPublicId() + " already contains resource with id " + resource.getPublicId());
        }
    }

    public void removeResource(Resource resource) {
        final var topicResource = getTopicResource(resource)
                .orElseThrow(() -> new ChildNotFoundException("Topic with id " + this.getPublicId() + " has no resource with id " + resource.getPublicId()));

        removeTopicResource(topicResource);

        if (topicResource.isPrimary()) resource.setRandomPrimaryTopic();
    }

    private Optional<TopicResource> getTopicResource(Resource resource) {
        return topicResources.stream()
                .filter(topicResource -> topicResource.getResource().isPresent())
                .filter(topicResource -> topicResource.getResource().get().equals(resource))
                .findFirst();
    }

    public Set<Topic> getSubtopics() {
        return childTopicSubtopics.stream()
                .map(TopicSubtopic::getSubtopic)
                .map(Optional::get)
                .collect(Collectors.toSet());
    }

    public Set<Topic> getParentTopics() {
        return parentTopicSubtopics.stream()
                .map(TopicSubtopic::getTopic)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
    }

    public Set<Resource> getResources() {
        return topicResources.stream()
                .map(TopicResource::getResource)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
    }

    public void setContentUri(URI contentUri) {
        this.contentUri = contentUri;
    }

    public URI getContentUri() {
        return contentUri;
    }

    public TopicTranslation addTranslation(String languageCode) {
        TopicTranslation topicTranslation = getTranslation(languageCode).orElse(null);
        if (topicTranslation != null) return topicTranslation;

        topicTranslation = new TopicTranslation(this, languageCode);
        translations.add(topicTranslation);
        return topicTranslation;
    }

    public Optional<TopicTranslation> getTranslation(String languageCode) {
        return translations.stream()
                .filter(translation -> translation.getLanguageCode().equals(languageCode))
                .findFirst();
    }

    public Set<TopicTranslation> getTranslations() {
        return translations;
    }

    public void addTranslation(TopicTranslation topicTranslation) {
        this.translations.add(topicTranslation);
        if (topicTranslation.getTopic() != this) {
            topicTranslation.setTopic(this);
        }
    }

    public void removeTranslation(TopicTranslation translation) {
        if (translation.getTopic() == this) {
            translations.remove(translation);
            if (translation.getTopic() == this) {
                translation.setTopic(null);
            }
        }
    }

    public void removeTranslation(String languageCode) {
        getTranslation(languageCode).ifPresent(this::removeTranslation);
    }

    public void setPrimarySubject(Subject subject) {
        // All other connections than to the provided subject is set to false, and provided connection set to true
        subjectTopics.forEach(st -> st.setPrimary(st.getSubject().filter(s -> s.equals(subject)).isPresent()));
    }

    public void setRandomPrimarySubject() {
        subjectTopics.stream()
                .map(SubjectTopic::getSubject)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .ifPresent(this::setPrimarySubject);
    }

    public void removeSubtopic(Topic subtopic) {
        final var topicSubtopic = getTopicSubtopic(subtopic)
                .orElseThrow(() -> new ChildNotFoundException("Topic " + this.getPublicId() + " has not subtopic with id " + subtopic.getPublicId()));

        removeChildTopicSubTopic(topicSubtopic);

        if (topicSubtopic.isPrimary()) {
            subtopic.setRandomPrimaryTopic();
        }
    }

    private Optional<TopicSubtopic> getTopicSubtopic(Topic subtopic) {
        return childTopicSubtopics.stream()
                .filter(topicSubtopic -> topicSubtopic.getSubtopic().isPresent())
                .filter(topicSubtopic -> topicSubtopic.getSubtopic().get().equals(subtopic))
                .findFirst();
    }

    public TopicFilter addFilter(Filter filter, Relevance relevance) {
        TopicFilter topicFilter = new TopicFilter();
        topicFilter.setTopic(this);
        topicFilter.setRelevance(relevance);
        topicFilter.setFilter(filter);

        addTopicFilter(topicFilter);

        return topicFilter;
    }

    public TopicResourceType addResourceType(ResourceType resourceType) {
        TopicResourceType topicResourceType = new TopicResourceType(this, resourceType);
        addTopicResourceType(topicResourceType);
        return topicResourceType;
    }

    public void removeResourceType(ResourceType resourceType) {
        TopicResourceType topicResourceType = getTopicResourceType(resourceType)
                .orElseThrow(() -> new ChildNotFoundException("Topic with id " + this.getPublicId() + " is not of type " + resourceType.getPublicId()));
        removeTopicResourceType(topicResourceType);
    }

    private Optional<TopicResourceType> getTopicResourceType(ResourceType resourceType) {
        for (TopicResourceType topicResourceType : topicResourceTypes) {
            if (resourceType.equals(topicResourceType.getResourceType().orElse(null))) {
                return Optional.of(topicResourceType);
            }
        }
        return Optional.empty();
    }

    public void addTopicFilter(TopicFilter topicFilter) {
        this.topicFilters.add(topicFilter);

        if (topicFilter.getTopic().orElse(null) != this) {
            topicFilter.setTopic(this);
        }
    }

    public void removeTopicFilter(TopicFilter topicFilter) {
        this.topicFilters.remove(topicFilter);

        if (topicFilter.getTopic().orElse(null) == this) {
            topicFilter.setTopic(null);
        }
    }

    public Set<TopicFilter> getTopicFilters() {
        return this.topicFilters;
    }

    public void setContext(boolean context) {
        this.context = context;
    }

    public boolean isContext() {
        return context;
    }

    @PreRemove
    void preRemove() {
        for (var topicSubtopic : parentTopicSubtopics.toArray(new TopicSubtopic[]{})) {
            removeParentTopicSubtopic(topicSubtopic);
        }

        for (var topicSubtopic : childTopicSubtopics.toArray(new TopicSubtopic[]{})) {
            removeChildTopicSubTopic(topicSubtopic);
        }

        for (var subjectTopic : subjectTopics.toArray(new SubjectTopic[]{})) {
            removeSubjectTopic(subjectTopic);
        }

        for (var topicResource : topicResources.toArray(new TopicResource[]{})) {
            removeTopicResource(topicResource);
        }

        for (var topicFilter : topicFilters.toArray(new TopicFilter[]{})) {
            removeTopicFilter(topicFilter);
        }
    }

}
