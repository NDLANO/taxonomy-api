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
public class Topic extends DomainObject {

    @OneToMany(mappedBy = "topic")
    Set<SubjectTopic> parentSubjects = new HashSet<>();

    @OneToMany(mappedBy = "topic")
    private Set<TopicSubtopic> subtopicConnections = new HashSet<>();

    @OneToMany(mappedBy = "subtopic")
    private Set<TopicSubtopic> parentConnections = new HashSet<>();

    @OneToMany(mappedBy = "topic")
    private Set<TopicResource> topicResources = new HashSet<>();

    @Column
    @Type(type = "no.ndla.taxonomy.service.hibernate.UriType")
    private URI contentUri;

    @OneToMany(mappedBy = "topic")
    Set<TopicTranslation> topicTranslations = new HashSet<>();

    public Topic() {
        setPublicId(URI.create("urn:topic:" + UUID.randomUUID()));
    }

    public Topic name(String name) {
        setName(name);
        return this;
    }

    public TopicSubtopic addSubtopic(Topic subtopic) {
        return addSubtopic(subtopic, false);

    }

    public TopicSubtopic addSubtopic(Topic subtopic, boolean primary) {
        Iterator<Topic> topics = getSubtopics();
        while (topics.hasNext()) {
            Topic t = topics.next();
            if (t.getId().equals(subtopic.getId())) {
                throw new DuplicateIdException("Topic with id " + getPublicId() + " already contains topic with id " + subtopic.getPublicId());
            }
        }

        TopicSubtopic topicSubtopic = new TopicSubtopic(this, subtopic);
        subtopic.setPrimaryParentTopic(primary, topicSubtopic);
        subtopic.addParent(topicSubtopic);
        subtopicConnections.add(topicSubtopic);
        return topicSubtopic;
    }

    private void addParent(TopicSubtopic topicSubtopic) {
        parentConnections.add(topicSubtopic);
    }

    private void setPrimaryParentTopic(boolean primary, TopicSubtopic topicSubtopic) {
        if (parentConnections.size() == 0) {
            topicSubtopic.setPrimary(true);
        } else {
            topicSubtopic.setPrimary(primary);
            if (primary) {
                invalidateOldPrimary();
            }
        }
    }

    private void invalidateOldPrimary() {
        for (TopicSubtopic tst : parentConnections) {
            tst.setPrimary(false);
        }
    }

    public TopicResource addResource(Resource resource) {
        return addResource(resource, false);
    }

    public TopicResource addResource(Resource resource, boolean primary) {
        Iterator<Resource> resources = getResources();
        while (resources.hasNext()) {
            Resource r = resources.next();
            if (r.getId().equals(resource.getId()))
                throw new DuplicateIdException("Topic with id " + getPublicId() + " already contains resource with id " + resource.getPublicId());
        }

        TopicResource topicResource = new TopicResource(this, resource);
        topicResources.add(topicResource);
        resource.addTopicResource(topicResource, primary);
        return topicResource;
    }

    public Iterator<Topic> getSubtopics() {
        Iterator<TopicSubtopic> iterator = subtopicConnections.iterator();

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

    public Iterator<TopicSubtopic> getParentTopics() {
        Iterator<TopicSubtopic> iterator = parentConnections.iterator();
        return new Iterator<TopicSubtopic>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public TopicSubtopic next() {
                return iterator.next();
            }
        };
    }

    public Iterator<Resource> getResources() {
        Iterator<TopicResource> iterator = topicResources.iterator();
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
        TopicTranslation topicTranslation = getTranslation(languageCode);
        if (topicTranslation != null) return topicTranslation;

        topicTranslation = new TopicTranslation(this, languageCode);
        topicTranslations.add(topicTranslation);
        return topicTranslation;
    }

    public TopicTranslation getTranslation(String languageCode) {
        return topicTranslations.stream()
                .filter(topicTranslation -> topicTranslation.getLanguageCode().equals(languageCode))
                .findFirst()
                .orElse(null);
    }

    public Iterator<TopicTranslation> getTranslations() {
        return topicTranslations.iterator();
    }

    public void removeTranslation(String languageCode) {
        TopicTranslation translation = getTranslation(languageCode);
        if (translation == null) return;
        topicTranslations.remove(translation);
    }

    public void addSubjectTopic(SubjectTopic subjectTopic, boolean primary) {
        setPrimaryParentSubject(primary, subjectTopic);
        parentSubjects.add(subjectTopic);
    }

    private void setPrimaryParentSubject(boolean primary, SubjectTopic subjectTopic) {
        if (parentSubjects.size() == 0) {
            subjectTopic.setPrimary(true);
        } else {
            subjectTopic.setPrimary(primary);
            if (primary) invalidateOldPrimarySubject();
        }
    }

    public Iterator<SubjectTopic> getParentSubjects() {
        Iterator<SubjectTopic> iterator = parentSubjects.iterator();
        return new Iterator<SubjectTopic>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public SubjectTopic next() {
                return iterator.next();
            }
        };
    }

    private void invalidateOldPrimarySubject() {
        for (SubjectTopic st : parentSubjects) {
            st.setPrimary(false);
        }
    }
}
