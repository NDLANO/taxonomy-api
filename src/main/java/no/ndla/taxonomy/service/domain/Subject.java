package no.ndla.taxonomy.service.domain;


import org.hibernate.annotations.Type;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

@Entity
public class Subject extends DomainObject {
    @Column
    @Type(type = "no.ndla.taxonomy.service.hibernate.UriType")
    private URI contentUri;

    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, orphanRemoval = true)
    public Set<SubjectTopic> topics = new HashSet<>();

    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<SubjectTranslation> translations = new HashSet<>();

    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL)
    Set<Filter> filters = new HashSet<>();

    public Subject() {
        setPublicId(URI.create("urn:subject:" + UUID.randomUUID()));
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
        SubjectTranslation subjectTranslation = getTranslation(languageCode);
        if (subjectTranslation != null) return subjectTranslation;

        subjectTranslation = new SubjectTranslation(this, languageCode);
        translations.add(subjectTranslation);
        return subjectTranslation;
    }

    public SubjectTranslation getTranslation(String languageCode) {
        return translations.stream()
                .filter(subjectTranslation -> subjectTranslation.getLanguageCode().equals(languageCode))
                .findFirst()
                .orElse(null);
    }

    public Iterator<SubjectTranslation> getTranslations() {
        return translations.iterator();
    }

    public void removeTranslation(String languageCode) {
        SubjectTranslation translation = getTranslation(languageCode);
        if (translation == null) return;
        translations.remove(translation);
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