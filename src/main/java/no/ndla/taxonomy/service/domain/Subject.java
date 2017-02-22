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
public class Subject extends DomainObject {
    @Column
    @Type(type = "no.ndla.taxonomy.service.hibernate.UriType")
    private URI contentUri;

    @OneToMany(mappedBy = "subject")
    Set<SubjectTopic> subjectTopics = new HashSet<>();

    @OneToMany(mappedBy = "subject")
    Set<SubjectTranslation> subjectTranslations = new HashSet<>();

    public Subject() {
        setPublicId(URI.create("urn:subject:" + UUID.randomUUID()));
    }

    public SubjectTopic addTopic(Topic topic) {
        return addTopic(topic, false);
    }

    public SubjectTopic addTopic(Topic topic, boolean primary) {
        Iterator<Topic> topics = getTopics();
        while (topics.hasNext()) {
            Topic t = topics.next();
            if (t.getId().equals(topic.getId()))
                throw new DuplicateIdException("Subject with id " + getPublicId() + " already contains topic with id " + topic.getPublicId());
        }

        SubjectTopic subjectTopic = new SubjectTopic(this, topic);
        subjectTopics.add(subjectTopic);
        topic.addSubjectTopic(subjectTopic, primary);
        topic.parentSubjects.add(subjectTopic);
        return subjectTopic;
    }

    public Iterator<Topic> getTopics() {
        Iterator<SubjectTopic> iterator = subjectTopics.iterator();

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
        subjectTranslations.add(subjectTranslation);
        return subjectTranslation;
    }

    public SubjectTranslation getTranslation(String languageCode) {
        return subjectTranslations.stream()
                .filter(subjectTranslation -> subjectTranslation.getLanguageCode().equals(languageCode))
                .findFirst()
                .orElse(null);
    }

    public Iterator<SubjectTranslation> getTranslations() {
        return subjectTranslations.iterator();
    }

    public void removeTranslation(String languageCode) {
        SubjectTranslation translation = getTranslation(languageCode);
        if (translation == null) return;
        subjectTranslations.remove(translation);
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
}