package no.ndla.taxonomy.domain;


import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.net.URI;
import java.util.*;

@Entity
public class Topic extends CachedUrlEntity {

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SubjectTopic> subjects = new HashSet<>();

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TopicSubtopic> subtopics = new HashSet<>();

    @OneToMany(mappedBy = "subtopic", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TopicSubtopic> parentTopics = new HashSet<>();

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TopicResource> resources = new HashSet<>();

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TopicResourceType> topicResourceTypes = new HashSet<>();

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TopicFilter> filters = new HashSet<>();

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
        return this.subjects;
    }

    public void addSubjectTopic(SubjectTopic subjectTopic) {
        this.subjects.add(subjectTopic);

        if (subjectTopic.getTopic() != this) {
            subjectTopic.setTopic(this);
        }
    }

    public void removeSubjectTopic(SubjectTopic subjectTopic) {
        this.subjects.remove(subjectTopic);

        var topic = subjectTopic.getTopic();

        if (topic == this) {
            subjectTopic.setTopic(null);
        }

        if (subjectTopic.isPrimary() && topic != null) {
            topic.setRandomPrimarySubject();
        }
    }

    public Set<TopicSubtopic> getChildrenTopicSubtopics() {
        return this.subtopics;
    }

    public Set<TopicSubtopic> getParentTopicSubtopics() {
        return this.parentTopics;
    }

    public void addChildTopicSubtopic(TopicSubtopic topicSubtopic) {
        this.subtopics.add(topicSubtopic);

        if (topicSubtopic.getTopic() != this) {
            topicSubtopic.setTopic(this);
        }
    }

    public void addParentTopicSubtopic(TopicSubtopic topicSubtopic) {
        this.parentTopics.add(topicSubtopic);

        if (topicSubtopic.getSubtopic() != this) {
            topicSubtopic.setSubtopic(this);
        }
    }

    public Set<TopicResource> getTopicResources() {
        return this.resources;
    }

    public void addTopicResource(TopicResource topicResource) {
        this.resources.add(topicResource);

        if (topicResource.getTopic() != this) {
            topicResource.setTopic(this);
        }
    }

    public Set<TopicResourceType> getTopicResourceTypes() {
        return this.topicResourceTypes;
    }

    public TopicSubtopic addSubtopic(Topic subtopic) {
        refuseIfDuplicateSubtopic(subtopic);

        TopicSubtopic topicSubtopic = new TopicSubtopic(this, subtopic);
        subtopic.parentTopics.add(topicSubtopic);
        subtopic.setPrimaryParentTopic(this);
        subtopics.add(topicSubtopic);
        return topicSubtopic;
    }

    public void addTopicResourceType(TopicResourceType topicResourceType) {
        this.topicResourceTypes.add(topicResourceType);

        if (topicResourceType.getTopic() != this) {
            topicResourceType.setTopic(this);
        }
    }

    public TopicSubtopic addSecondarySubtopic(Topic subtopic) {
        refuseIfDuplicateSubtopic(subtopic);

        TopicSubtopic topicSubtopic = new TopicSubtopic(this, subtopic);
        topicSubtopic.setPrimary(false);
        subtopic.parentTopics.add(topicSubtopic);
        subtopics.add(topicSubtopic);
        return topicSubtopic;
    }

    public boolean hasSingleSubject() {
        return subjects.size() == 1;
    }

    private void refuseIfDuplicateSubtopic(Topic subtopic) {
        Iterator<Topic> topics = getSubtopics();
        while (topics.hasNext()) {
            Topic t = topics.next();
            if (t.getId().equals(subtopic.getId())) {
                throw new DuplicateIdException("Topic with id " + getPublicId() + " already contains topic with id " + subtopic.getPublicId());
            }
        }
    }

    public Topic getPrimaryParentTopic() {
        for (TopicSubtopic parentTopic : parentTopics) {
            if (parentTopic.isPrimary()) return parentTopic.getTopic();
        }
        return null;
    }

    public void setPrimaryParentTopic(Topic topic) {
        TopicSubtopic topicSubtopic = getParentTopic(topic);
        if (null == topicSubtopic) throw new ParentNotFoundException(this, topic);

        parentTopics.forEach(st -> st.setPrimary(false));
        topicSubtopic.setPrimary(true);
    }

    private void setRandomPrimaryTopic(Topic subtopic) {
        if (subtopic.parentTopics.size() == 0) return;
        setPrimaryParentTopic(subtopic.parentTopics.iterator().next().getTopic());
    }

    private TopicSubtopic getParentTopic(Topic parentTopic) {
        for (TopicSubtopic topicSubtopic : parentTopics) {
            if (topicSubtopic.getTopic().equals(parentTopic)) {
                return topicSubtopic;
            }
        }
        return null;
    }

    public TopicResource addResource(Resource resource) {
        refuseIfDuplicateResource(resource);

        TopicResource topicResource = new TopicResource(this, resource);
        this.resources.add(topicResource);
        resource.getTopicResources().add(topicResource);
        resource.setPrimaryTopic(this);
        return topicResource;
    }

    public TopicResource addSecondaryResource(Resource resource) {
        refuseIfDuplicateResource(resource);

        TopicResource topicResource = new TopicResource(this, resource);
        topicResource.setPrimary(false);
        this.resources.add(topicResource);
        resource.getTopicResources().add(topicResource);
        return topicResource;
    }

    private void refuseIfDuplicateResource(Resource resource) {
        Iterator<Resource> resources = getResources();
        while (resources.hasNext()) {
            Resource r = resources.next();
            if (r.getId().equals(resource.getId()))
                throw new DuplicateIdException("Topic with id " + getPublicId() + " already contains resource with id " + resource.getPublicId());
        }
    }

    public void removeResource(Resource resource) {
        TopicResource topicResource = getResource(resource);
        if (topicResource == null)
            throw new ChildNotFoundException("Topic with id " + this.getPublicId() + " has no resource with id " + topicResource.getResource());
        resource.getTopicResources().remove(topicResource);
        resources.remove(topicResource);
        if (topicResource.isPrimary()) resource.setRandomPrimaryTopic();
    }

    private TopicResource getResource(Resource resource) {
        for (TopicResource topicResource : resources) {
            if (topicResource.getResource().equals(resource)) {
                return topicResource;
            }
        }
        return null;
    }

    public Iterator<Topic> getSubtopics() {
        Iterator<TopicSubtopic> iterator = subtopics.iterator();

        return new Iterator<Topic>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Topic next() {
                return iterator.next().getSubtopic();
            }
        };
    }

    public Iterator<Topic> getParentTopics() {
        Iterator<TopicSubtopic> iterator = parentTopics.iterator();
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

    public Iterator<Resource> getResources() {
        Iterator<TopicResource> iterator = resources.iterator();
        return new Iterator<Resource>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Resource next() {
                return iterator.next().getResource();
            }
        };
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

    public Iterator<TopicTranslation> getTranslations() {
        return translations.iterator();
    }

    public void removeTranslation(String languageCode) {
        getTranslation(languageCode).ifPresent(translations::remove);
    }

    public void setPrimarySubject(Subject subject) {
        SubjectTopic subjectTopic = getSubject(subject);
        if (null == subjectTopic) throw new ParentNotFoundException(this, subject);

        subjects.forEach(st -> st.setPrimary(false));
        subjectTopic.setPrimary(true);
    }

    public void setRandomPrimarySubject() {
        if (subjects.size() == 0) return;
        for (var subjectTopic : subjects) {
            if (subjectTopic.getSubject() != null && subjectTopic.getTopic() != null) {
                subjectTopic.setPrimary(true);
                return;
            }
        }
    }

    private SubjectTopic getSubject(Subject subject) {
        for (SubjectTopic subjectTopic : subjects) {
            if (subjectTopic.getSubject().equals(subject)) {
                return subjectTopic;
            }
        }
        return null;
    }

    public Iterator<Subject> getSubjects() {
        Iterator<SubjectTopic> iterator = subjects.iterator();
        return new Iterator<Subject>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Subject next() {
                return iterator.next().getSubject();
            }
        };
    }

    public void removeSubtopic(Topic subtopic) {
        TopicSubtopic topicSubtopic = getSubtopic(subtopic);
        if (topicSubtopic == null)
            throw new ChildNotFoundException("Topic " + this.getPublicId() + " has not subtopic with id " + subtopic.getPublicId());
        subtopic.parentTopics.remove(topicSubtopic);
        subtopics.remove(topicSubtopic);
        if (topicSubtopic.isPrimary()) subtopic.setRandomPrimaryTopic(subtopic);
    }

    private TopicSubtopic getSubtopic(Topic subtopic) {
        for (TopicSubtopic topicSubtopic : subtopics) {
            if (topicSubtopic.getSubtopic().equals(subtopic)) {
                return topicSubtopic;
            }
        }
        return null;
    }

    public TopicFilter addFilter(Filter filter, Relevance relevance) {
        TopicFilter topicFilter = new TopicFilter(this, filter, relevance);
        addTopicFilter(topicFilter);
        return topicFilter;
    }

    public void removeFilter(Filter filter) {
        TopicFilter topicFilter = getFilter(filter);
        if (filter == null) {
            throw new ChildNotFoundException("Topic with id " + this.getPublicId() + " does not have topic-filter " + topicFilter.getPublicId());
        }
        removeTopicFilter(topicFilter);
    }

    private TopicFilter getFilter(Filter filter) {
        for (TopicFilter rf : filters) {
            if (rf.getFilter().isPresent() && rf.getFilter().get().equals(filter)) {
                return rf;
            }
        }
        return null;
    }

    public TopicResourceType addResourceType(ResourceType resourceType) {
        TopicResourceType topicResourceType = new TopicResourceType(this, resourceType);
        topicResourceTypes.add(topicResourceType);
        return topicResourceType;
    }

    public void removeResourceType(ResourceType resourceType) {
        TopicResourceType topicResourceType = getResourceType(resourceType);
        if (topicResourceType == null) {
            throw new ChildNotFoundException("Topic with id " + this.getPublicId() + " is not of type " + resourceType.getPublicId());
        }
        topicResourceTypes.remove(topicResourceType);
    }

    private TopicResourceType getResourceType(ResourceType resourceType) {
        for (TopicResourceType topicResourceType : topicResourceTypes) {
            if (topicResourceType.getResourceType().equals(resourceType)) return topicResourceType;
        }
        return null;
    }

    public void addTopicFilter(TopicFilter topicFilter) {
        this.filters.add(topicFilter);

        if (topicFilter.getTopic().orElse(null) != this) {
            topicFilter.setTopic(this);
        }
    }

    public void removeTopicFilter(TopicFilter topicFilter) {
        this.filters.remove(topicFilter);

        if (topicFilter.getTopic().orElse(null) == this) {
            topicFilter.setTopic(null);
        }
    }

    public Set<TopicFilter> getTopicFilters() {
        return this.filters;
    }

    public void setContext(boolean context) {
        this.context = context;
    }

    public boolean isContext() {
        return context;
    }

    @PreRemove
    void preRemove() {
        for (TopicSubtopic edge : parentTopics.toArray(new TopicSubtopic[]{})) {
            edge.getTopic().removeSubtopic(this);
        }
        for (TopicSubtopic edge : subtopics.toArray(new TopicSubtopic[]{})) {
            this.removeSubtopic(edge.getSubtopic());
        }
        for (SubjectTopic edge : subjects.toArray(new SubjectTopic[]{})) {
            this.removeSubjectTopic(edge);
        }
        for (TopicResource edge : resources.toArray(new TopicResource[]{})) {
            this.removeResource(edge.getResource());
        }

        for (var topicFilter : filters.toArray()) {
            this.removeTopicFilter((TopicFilter) topicFilter);
        }
    }

}
