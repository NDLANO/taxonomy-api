package no.ndla.taxonomy.domain;


import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.net.URI;
import java.util.*;

@Entity
public class Subject extends CachedUrlEntity {
    @Column
    @Type(type = "no.ndla.taxonomy.hibernate.UriType")
    private URI contentUri;

    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SubjectTopic> topics = new HashSet<>();

    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SubjectTranslation> translations = new HashSet<>();

    @OneToMany(mappedBy = "subject", orphanRemoval = true)
    private Set<Filter> filters = new HashSet<>();

    public Subject() {
        setPublicId(URI.create("urn:subject:" + UUID.randomUUID()));
    }

    public void addSubjectTopic(SubjectTopic subjectTopic) {
        this.topics.add(subjectTopic);

        if (subjectTopic.getSubject() != this) {
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
        return this.topics;
    }

    public void addFilter(Filter filter) {
        this.filters.add(filter);

        if (filter.getSubject() != this) {
            filter.setSubject(this);
        }
    }

    public void removeSubjectTopic(SubjectTopic subjectTopic) {
        this.topics.remove(subjectTopic);

        if (subjectTopic.getSubject() == this) {
            subjectTopic.setSubject(null);
        }

        if (subjectTopic.isPrimary() && subjectTopic.getTopic() != null) {
            subjectTopic.getTopic().setRandomPrimarySubject();
        }
    }

    public Set<Filter> getFilters() {
        return filters;
    }

    public void removeFilter(Filter filter) {
        if (this.filters.contains(filter)) {
            this.filters.remove(filter);

            if (filter.getSubject() == this) {
                filter.setSubject(null);
            }
        }
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

        this.removeSubjectTopic(subjectTopic);
    }

    private SubjectTopic getTopic(Topic topic) {
        for (SubjectTopic subjectTopic : topics) {
            if (subjectTopic.getTopic().getPublicId().equals(topic.getPublicId())) return subjectTopic;
        }
        return null;
    }

    @PreRemove
    void preRemove() {
        final SubjectTopic[] topics = this.topics.toArray(new SubjectTopic[]{});
        for (SubjectTopic topicSubtopic : topics) {
            Topic topic = topicSubtopic.getTopic();
            this.removeTopic(topic);
        }
    }
}