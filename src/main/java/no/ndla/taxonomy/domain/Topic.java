package no.ndla.taxonomy.domain;


import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.net.URI;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Entity
public class Topic extends EntityWithPath {

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

    @Override
    public Set<EntityWithPathConnection> getParentConnections() {
        return Stream.concat(parentTopicSubtopics.stream(), subjectTopics.stream())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<EntityWithPathConnection> getChildConnections() {
        return Stream.concat(childTopicSubtopics.stream(), topicResources.stream())
                .collect(Collectors.toUnmodifiableSet());
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
        if (subjectTopic.getTopic().orElse(null) != this) {
            throw new IllegalArgumentException("Topic must be set on SubjectTopic before associating with Topic");
        }

        this.subjectTopics.add(subjectTopic);
    }

    public void removeSubjectTopic(SubjectTopic subjectTopic) {
        this.subjectTopics.remove(subjectTopic);

        var topic = subjectTopic.getTopic().orElse(null);

        if (topic == this) {
            subjectTopic.disassociate();
        }
    }

    public Set<TopicSubtopic> getChildrenTopicSubtopics() {
        return this.childTopicSubtopics.stream().collect(Collectors.toUnmodifiableSet());
    }

    public Set<TopicSubtopic> getParentTopicSubtopics() {
        return this.parentTopicSubtopics.stream().collect(Collectors.toUnmodifiableSet());
    }

    public void addChildTopicSubtopic(TopicSubtopic topicSubtopic) {
        if (topicSubtopic.getTopic().orElse(null) != this) {
            throw new IllegalArgumentException("Parent topic must be set on TopicSubtopic before associating with Topic");
        }

        this.childTopicSubtopics.add(topicSubtopic);
    }

    public void removeChildTopicSubTopic(TopicSubtopic topicSubtopic) {
        this.childTopicSubtopics.remove(topicSubtopic);

        topicSubtopic.disassociate();
    }

    public void addParentTopicSubtopic(TopicSubtopic topicSubtopic) {
        if (topicSubtopic.getSubtopic().orElse(null) != this) {
            throw new IllegalArgumentException("Subtopic must be set on TopicSubtopic before associating with Topic");
        }

        this.parentTopicSubtopics.add(topicSubtopic);
    }

    public void removeParentTopicSubtopic(TopicSubtopic topicSubtopic) {
        this.parentTopicSubtopics.remove(topicSubtopic);

        topicSubtopic.disassociate();
    }

    public Set<TopicResource> getTopicResources() {
        return this.topicResources.stream().collect(Collectors.toUnmodifiableSet());
    }

    public void addTopicResource(TopicResource topicResource) {
        if (topicResource.getTopic().orElse(null) != this) {
            throw new IllegalArgumentException("TopicResource must have Topic set before it can be associated with Topic");
        }

        this.topicResources.add(topicResource);
    }

    public void removeTopicResource(TopicResource topicResource) {
        this.topicResources.remove(topicResource);

        final var resource = topicResource.getResource();

        if (topicResource.getTopic().orElse(null) == this) {
            topicResource.disassociate();
        }
    }

    public Set<TopicResourceType> getTopicResourceTypes() {
        return this.topicResourceTypes.stream().collect(Collectors.toUnmodifiableSet());
    }

    public void addTopicResourceType(TopicResourceType topicResourceType) {
        if (topicResourceType.getTopic().orElse(null) != this) {
            throw new IllegalArgumentException("TopicResourceType must have Topic set before associating with Topic");
        }

        this.topicResourceTypes.add(topicResourceType);
    }

    public void removeTopicResourceType(TopicResourceType topicResourceType) {
        this.topicResourceTypes.remove(topicResourceType);

        if (topicResourceType.getTopic().orElse(null) == this) {
            topicResourceType.disassociate();
        }
    }

    public boolean hasSingleSubject() {
        return subjectTopics.size() == 1;
    }

    public Optional<Topic> getPrimaryParentTopic() {
        for (TopicSubtopic parentTopic : parentTopicSubtopics) {
            if (parentTopic.isPrimary()) return parentTopic.getTopic();
        }
        return Optional.empty();
    }

    public Set<Topic> getSubtopics() {
        return childTopicSubtopics.stream()
                .map(TopicSubtopic::getSubtopic)
                .map(Optional::get)
                .collect(Collectors.toUnmodifiableSet());
    }

    public Set<Topic> getParentTopics() {
        return parentTopicSubtopics.stream()
                .map(TopicSubtopic::getTopic)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toUnmodifiableSet());
    }

    public Set<Resource> getResources() {
        return topicResources.stream()
                .map(TopicResource::getResource)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toUnmodifiableSet());
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
        return translations.stream().collect(Collectors.toUnmodifiableSet());
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

    public TopicFilter addFilter(Filter filter, Relevance relevance) {
        return TopicFilter.create(this, filter, relevance);
    }

    public TopicResourceType addResourceType(ResourceType resourceType) {
        return TopicResourceType.create(this, resourceType);
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
        if (topicFilter.getTopic().orElse(null) != this) {
            throw new IllegalArgumentException("TopicFilter must have Topic set before associating with Topic");
        }

        this.topicFilters.add(topicFilter);
    }

    public void removeTopicFilter(TopicFilter topicFilter) {
        this.topicFilters.remove(topicFilter);

        if (topicFilter.getTopic().orElse(null) == this) {
            topicFilter.disassociate();
        }
    }

    public Set<TopicFilter> getTopicFilters() {
        return this.topicFilters.stream().collect(Collectors.toUnmodifiableSet());
    }

    public void setContext(boolean context) {
        this.context = context;
    }

    public boolean isContext() {
        return context;
    }

    @PreRemove
    void preRemove() {
        new HashSet<>(subjectTopics).forEach(SubjectTopic::disassociate);
        new HashSet<>(childTopicSubtopics).forEach(TopicSubtopic::disassociate);
        new HashSet<>(parentTopicSubtopics).forEach(TopicSubtopic::disassociate);
        new HashSet<>(topicResources).forEach(TopicResource::disassociate);
        new HashSet<>(topicResourceTypes).forEach(TopicResourceType::disassociate);
        new HashSet<>(topicFilters).forEach(TopicFilter::disassociate);
    }
}
