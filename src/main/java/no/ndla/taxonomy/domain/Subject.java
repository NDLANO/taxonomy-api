package no.ndla.taxonomy.domain;


import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@Entity
public class Subject extends EntityWithPath {
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

    @Override
    public Set<EntityWithPathConnection> getParentConnections() {
        return Set.of();
    }

    @Override
    public Set<EntityWithPathConnection> getChildConnections() {
        return subjectTopics.stream().collect(Collectors.toUnmodifiableSet());
    }

    public void addSubjectTopic(SubjectTopic subjectTopic) {
        this.subjectTopics.add(subjectTopic);

        if (subjectTopic.getSubject().orElse(null) != this) {
            throw new IllegalArgumentException("Subject must be set on SubjectTopic before associating with Subject");
        }
    }

    public Set<SubjectTopic> getSubjectTopics() {
        return this.subjectTopics.stream().collect(Collectors.toUnmodifiableSet());
    }

    public void addFilter(Filter filter) {
        this.filters.add(filter);

        if (filter.getSubject().orElse(null) != this) {
            filter.setSubject(this);
        }
    }

    public void removeSubjectTopic(SubjectTopic subjectTopic) {
        this.subjectTopics.remove(subjectTopic);

        if (subjectTopic.getSubject().orElse(null) == this) {
            subjectTopic.disassociate();
        }
    }

    public Set<Filter> getFilters() {
        return filters.stream().collect(Collectors.toUnmodifiableSet());
    }

    public void removeFilter(Filter filter) {
        if (this.filters.contains(filter)) {
            this.filters.remove(filter);

            if (filter.getSubject().orElse(null) == this) {
                filter.setSubject(null);
            }
        }
    }

    public Collection<Topic> getTopics() {
        return subjectTopics.stream()
                .map(SubjectTopic::getTopic)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toUnmodifiableSet());
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
        return translations.stream().collect(Collectors.toUnmodifiableSet());
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

    @PreRemove
    void preRemove() {
        new HashSet<>(subjectTopics).forEach(SubjectTopic::disassociate);
    }
}