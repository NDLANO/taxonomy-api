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
    Set<SubjectTopic> subjects = new HashSet<>();

    @OneToMany(mappedBy = "topic")
    private Set<TopicSubtopic> subtopics = new HashSet<>();

    @OneToMany(mappedBy = "subtopic")
    private Set<TopicSubtopic> parentTopics = new HashSet<>();

    @OneToMany(mappedBy = "topic")
    private Set<TopicResource> resources = new HashSet<>();

    @Column
    @Type(type = "no.ndla.taxonomy.service.hibernate.UriType")
    private URI contentUri;

    @OneToMany(mappedBy = "topic")
    Set<TopicTranslation> translations = new HashSet<>();

    public Topic() {
        setPublicId(URI.create("urn:topic:" + UUID.randomUUID()));
    }

    public Topic name(String name) {
        setName(name);
        return this;
    }

    public TopicSubtopic addSubtopic(Topic subtopic) {
        refuseIfDuplicateSubtopic(subtopic);

        TopicSubtopic topicSubtopic = new TopicSubtopic(this, subtopic);
        subtopic.parentTopics.add(topicSubtopic);
        subtopics.add(topicSubtopic);
        if (subtopic.hasSingleParentTopic()) subtopic.setPrimaryParentTopic(this);
        return topicSubtopic;
    }

    public boolean hasSingleParentTopic() {
        return parentTopics.size() == 1;
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
        if (resource.hasSingleParentTopic()) resource.setPrimaryTopic(this);
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

    public Iterator<TopicSubtopic> getParentTopics() {
        Iterator<TopicSubtopic> iterator = parentTopics.iterator();
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
        TopicTranslation topicTranslation = getTranslation(languageCode);
        if (topicTranslation != null) return topicTranslation;

        topicTranslation = new TopicTranslation(this, languageCode);
        translations.add(topicTranslation);
        return topicTranslation;
    }

    public TopicTranslation getTranslation(String languageCode) {
        return translations.stream()
                .filter(translation -> translation.getLanguageCode().equals(languageCode))
                .findFirst()
                .orElse(null);
    }

    public Iterator<TopicTranslation> getTranslations() {
        return translations.iterator();
    }

    public void removeTranslation(String languageCode) {
        TopicTranslation translation = getTranslation(languageCode);
        if (translation == null) return;
        translations.remove(translation);
    }

    public void setPrimarySubject(Subject subject) {
        SubjectTopic subjectTopic = getSubject(subject);
        if (null == subjectTopic) throw new ParentNotFoundException(this, subject);

        subjects.forEach(st -> st.setPrimary(false));
        subjectTopic.setPrimary(true);
    }

    public void setRandomPrimarySubject() {
        if (subjects.size() == 0) return;
        getSubjects().next().setPrimary(true);
    }

    private SubjectTopic getSubject(Subject subject) {
        for (SubjectTopic subjectTopic : subjects) {
            if (subjectTopic.getSubject().equals(subject)) {
                return subjectTopic;
            }
        }
        return null;
    }


    public Iterator<SubjectTopic> getSubjects() {
        Iterator<SubjectTopic> iterator = subjects.iterator();
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
}
