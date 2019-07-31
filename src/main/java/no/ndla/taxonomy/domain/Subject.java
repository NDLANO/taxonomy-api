package no.ndla.taxonomy.domain;


import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@Entity
public class Subject extends CachedUrlEntity {
    @Column
    @Type(type = "no.ndla.taxonomy.hibernate.UriType")
    private URI contentUri;

    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SubjectTopic> subjectTopics = new HashSet<>();

    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SubjectTranslation> translations = new HashSet<>();

    @OneToMany(mappedBy = "subject", orphanRemoval = true)
    private Set<Filter> filters = new HashSet<>();

    public Subject() {
        setPublicId(URI.create("urn:subject:" + UUID.randomUUID()));
    }

    public void addSubjectTopic(SubjectTopic subjectTopic) {
        this.subjectTopics.add(subjectTopic);

        if (subjectTopic.getSubject().orElse(null) != this) {
            subjectTopic.setSubject(this);
        }
    }

    public SubjectTopic addTopic(Topic topic) {
        refuseIfDuplicate(topic);

        SubjectTopic subjectTopic = new SubjectTopic(this, topic);
        this.addSubjectTopic(subjectTopic);
        if (topic.hasSingleSubject()) topic.setPrimarySubject(this);
        return subjectTopic;
    }

    public Set<SubjectTopic> getSubjectTopics() {
        return this.subjectTopics;
    }

    public void addFilter(Filter filter) {
        this.filters.add(filter);

        if (filter.getSubject().orElse(null) != this) {
            filter.setSubject(this);
        }
    }

    public void removeSubjectTopic(SubjectTopic subjectTopic) {
        final var topicToRemove = subjectTopic.getTopic().orElse(null);

        this.subjectTopics.remove(subjectTopic);

        if (subjectTopic.getSubject().orElse(null) == this) {
            subjectTopic.setSubject(null);
        }
    }

    public Set<Filter> getFilters() {
        return filters;
    }

    public void removeFilter(Filter filter) {
        if (this.filters.contains(filter)) {
            this.filters.remove(filter);

            if (filter.getSubject().orElse(null) == this) {
                filter.setSubject(null);
            }
        }
    }

    private void refuseIfDuplicate(Topic topic) {
        if (getTopics().contains(topic)) {
            throw new DuplicateIdException("Subject with id " + getPublicId() + " already contains topic with id " + topic.getPublicId());
        }
    }

    public Collection<Topic> getTopics() {
        return subjectTopics.stream()
                .map(SubjectTopic::getTopic)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
    }

    public SubjectTranslation addTranslation(String languageCode) {
        SubjectTranslation subjectTranslation = getTranslation(languageCode).orElse(null);
        if (subjectTranslation != null) return subjectTranslation;

        subjectTranslation = new SubjectTranslation(this, languageCode);
        translations.add(subjectTranslation);
        return subjectTranslation;
    }

    public Optional<SubjectTranslation> getTranslation(String languageCode) {
        return translations.stream()
                .filter(subjectTranslation -> subjectTranslation.getLanguageCode().equals(languageCode))
                .findFirst();
    }

    public Set<SubjectTranslation> getTranslations() {
        return translations;
    }

    public void removeTranslation(String languageCode) {
        getTranslation(languageCode).ifPresent(this::removeTranslation);
    }

    public void addTranslation(SubjectTranslation subjectTranslation) {
        this.translations.add(subjectTranslation);
        if (subjectTranslation.getSubject() != this) {
            subjectTranslation.setSubject(this);
        }
    }

    public void removeTranslation(SubjectTranslation translation) {
        if (translation.getSubject() == this) {
            translations.remove(translation);
            if (translation.getSubject() == this) {
                translation.setSubject(null);
            }
        }
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
        // Removes SubjectTopic that references requested Topic if exists, otherwise throws exception
        subjectTopics.stream()
                .filter(subjectTopic -> topic.equals(subjectTopic.getTopic().orElse(null)))
                .findFirst()
                .ifPresentOrElse(
                        this::removeSubjectTopic,
                        () -> {
                            throw new ChildNotFoundException("Subject " + this.getPublicId() + " has no topic with id " + topic.getPublicId());
                        }
                );
    }

    @PreRemove
    void preRemove() {
        for (var subjectTopic : subjectTopics.toArray()) {
            removeSubjectTopic((SubjectTopic) subjectTopic);
        }
    }
}