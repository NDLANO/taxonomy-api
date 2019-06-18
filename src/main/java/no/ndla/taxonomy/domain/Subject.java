package no.ndla.taxonomy.domain;


import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.*;
import java.net.URI;
import java.time.Instant;
import java.util.*;

@Entity
public class Subject extends DomainObject implements ResolvablePathEntity {
    @Id
    private Integer id;

    @UpdateTimestamp
    private Instant updatedAt;

    @Column
    @Type(type = "no.ndla.taxonomy.hibernate.UriType")
    private URI contentUri;

    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, orphanRemoval = true)
    public Set<SubjectTopic> topics = new HashSet<>();

    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<SubjectTranslation> translations = new HashSet<>();

    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<Filter> filters = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "publicId", referencedColumnName = "publicId", insertable = false, updatable = false)
    private Set<ResolvedPath> resolvedPaths;

    public Set<ResolvedPath> getResolvedPaths() {
        return resolvedPaths;
    }

    public Optional<ResolvedPath> getPrimaryResolvedPath() {
        return resolvedPaths.stream().filter(ResolvedPath::isPrimary).findFirst();
    }

    public Subject() {
        setPublicId(URI.create("urn:subject:" + UUID.randomUUID()));
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public String getType() {
        return "subject";
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
        return Set.of(
                GeneratedPath.builder()
                        .setSubPath(this.getPublicId().getSchemeSpecificPart())
                        .setIsPrimary(true)
                        .build()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Set<GeneratedPath> generatePaths() {
        return generatePaths(0);
    }

    @Override
    public Set<ResolvablePathEntity> getChildren() {
        final var children = new HashSet<ResolvablePathEntity>();

        getTopics().forEachRemaining(children::add);

        return children;
    }

    public SubjectTopic addTopic(Topic topic) {
        refuseIfDuplicate(topic);

        SubjectTopic subjectTopic = new SubjectTopic(this, topic);
        this.topics.add(subjectTopic);
        topic.subjects.add(subjectTopic);
        if (topic.hasSingleSubject()) topic.setPrimarySubject(this);
        return subjectTopic;
    }

    public void addFilter(Filter filter) {
        this.filters.add(filter);
        filter.setSubject(this);
    }

    private void refuseIfDuplicate(Topic topic) {
        Iterator<Topic> topics = getTopics();
        while (topics.hasNext()) {
            Topic t = topics.next();
            if (t.getId().equals(topic.getId()))
                throw new DuplicateIdException("Subject with id " + getPublicId() + " already contains topic with id " + topic.getPublicId());
        }
    }

    public Iterator<Topic> getTopics() {
        Iterator<SubjectTopic> iterator = topics.iterator();

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

    public SubjectTranslation addTranslation(String languageCode) {
        final var existingSubjectTranslation = getTranslation(languageCode);
        if (existingSubjectTranslation.isPresent()) {
            return existingSubjectTranslation.get();
        }

        final var subjectTranslation = new SubjectTranslation(this, languageCode);
        translations.add(subjectTranslation);
        return subjectTranslation;
    }

    public Optional<SubjectTranslation> getTranslation(String languageCode) {
        return translations.stream()
                .filter(subjectTranslation -> subjectTranslation.getLanguageCode().equals(languageCode))
                .findFirst();
    }

    public Iterator<SubjectTranslation> getTranslations() {
        return translations.iterator();
    }

    public void removeTranslation(String languageCode) {
        getTranslation(languageCode).ifPresent(translations::remove);
    }

    public Subject name(String name) {
        setName(name);
        return this;
    }

    public URI getContentUri() {
        return contentUri;
    }

    public void setContentUri(URI contentUri) {
        this.contentUri = contentUri;
    }

    public void removeTopic(Topic topic) {
        SubjectTopic subjectTopic = getTopic(topic);
        if (subjectTopic == null) {
            throw new ChildNotFoundException("Subject " + this.getPublicId() + " has no topic with id " + topic.getPublicId());
        }
        topic.subjects.remove(subjectTopic);
        topics.remove(subjectTopic);
        if (subjectTopic.isPrimary()) topic.setRandomPrimarySubject();
    }

    private SubjectTopic getTopic(Topic topic) {
        for (SubjectTopic subjectTopic : topics) {
            if (subjectTopic.getTopic().getPublicId().equals(topic.getPublicId())) return subjectTopic;
        }
        return null;
    }
}