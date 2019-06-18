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
public class Topic extends DomainObject implements ResolvablePathEntity {
    @Id
    private Integer id;

    @UpdateTimestamp
    private Instant updatedAt;

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true)
    public Set<SubjectTopic> subjects = new HashSet<>();

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true)
    public Set<TopicSubtopic> subtopics = new HashSet<>();

    @OneToMany(mappedBy = "subtopic", cascade = CascadeType.ALL, orphanRemoval = true)
    public Set<TopicSubtopic> parentTopics = new HashSet<>();

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true)
    public Set<TopicResource> resources = new HashSet<>();

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true)
    public Set<TopicFilter> filters = new HashSet<>();

    @Column
    @Type(type = "no.ndla.taxonomy.hibernate.UriType")
    private URI contentUri;

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<TopicTranslation> translations = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "publicId", referencedColumnName = "publicId", insertable = false, updatable = false)
    private Set<ResolvedPath> resolvedPaths;

    public Set<ResolvedPath> getResolvedPaths() {
        return resolvedPaths;
    }

    public Optional<ResolvedPath> getPrimaryResolvedPath() {
        return resolvedPaths.stream().filter(ResolvedPath::isPrimary).findFirst();
    }

    @Column
    private boolean context;

    public Topic() {
        setPublicId(URI.create("urn:topic:" + UUID.randomUUID()));
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public String getType() {
        return "topic";
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
    public Set<GeneratedPath> generatePaths(int iterations) {
        final Set<GeneratedPath> generatedPaths = new HashSet<>();

        final int currentIterations = iterations + 1;

        if (currentIterations > 20) {
            // Probably a looped topic path, in this case return no URLs since this path it will never resolve to anything
            return Set.of();
        }

        generatedPaths.addAll(subjects
                .stream()
                .flatMap(subjectTopic -> subjectTopic
                        .getSubject()
                        .generatePaths(currentIterations)
                        .stream()
                        .map(generatedPath ->
                                GeneratedPath.builder()
                                        .setParentPath(generatedPath)
                                        .setIsPrimary(subjectTopic.isPrimary())
                                        .setSubPath(this.getPublicId().getSchemeSpecificPart())
                                        .build()
                        ))
                .collect(Collectors.toSet()));

        generatedPaths.addAll(parentTopics
                .stream()
                .flatMap(topicSubtopic -> topicSubtopic
                        .getTopic()
                        .generatePaths(currentIterations)
                        .stream()
                        .map(generatedPath ->
                                GeneratedPath.builder()
                                        .setParentPath(generatedPath)
                                        .setIsPrimary(topicSubtopic.isPrimary())
                                        .setSubPath(this.getPublicId().getSchemeSpecificPart())
                                        .setParentId(topicSubtopic.getTopic().getPublicId())
                                        .build()
                        ))
                .collect(Collectors.toSet()));

        return generatedPaths;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<GeneratedPath> generatePaths() {
        return generatePaths(0);
    }

    @Override
    public Set<ResolvablePathEntity> getChildren() {
        final var children = new HashSet<ResolvablePathEntity>();

        getSubtopics().forEachRemaining(children::add);
        getResources().forEachRemaining(children::add);

        return children;
    }

    public Topic name(String name) {
        setName(name);
        return this;
    }

    public TopicSubtopic addSubtopic(Topic subtopic) {
        refuseIfDuplicateSubtopic(subtopic);

        TopicSubtopic topicSubtopic = new TopicSubtopic(this, subtopic);
        subtopic.parentTopics.add(topicSubtopic);
        subtopic.setPrimaryParentTopic(this);
        subtopics.add(topicSubtopic);
        return topicSubtopic;
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
        resource.topics.add(topicResource);
        resource.setPrimaryTopic(this);
        return topicResource;
    }

    public TopicResource addSecondaryResource(Resource resource) {
        refuseIfDuplicateResource(resource);

        TopicResource topicResource = new TopicResource(this, resource);
        topicResource.setPrimary(false);
        this.resources.add(topicResource);
        resource.topics.add(topicResource);
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
        resource.topics.remove(topicResource);
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
        final var existingTranslation = getTranslation(languageCode);

        if (existingTranslation.isPresent()) {
            return existingTranslation.get();
        }

        final var topicTranslation = new TopicTranslation(this, languageCode);
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
        subjects.iterator().next().setPrimary(true);
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
        filters.add(topicFilter);
        return topicFilter;
    }

    public void removeFilter(Filter filter) {
        TopicFilter topicFilter = getFilter(filter);
        if (filter == null) {
            throw new ChildNotFoundException("Topic with id " + this.getPublicId() + " does not have topic-filter " + topicFilter.getPublicId());
        }
        filters.remove(topicFilter);
    }

    private TopicFilter getFilter(Filter filter) {
        for (TopicFilter rf : filters) {
            if (rf.getFilter().equals(filter)) return rf;
        }
        return null;
    }

    public void setContext(boolean context) {
        this.context = context;
    }

    public boolean isContext() {
        return context;
    }

}
