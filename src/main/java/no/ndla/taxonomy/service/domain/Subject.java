package no.ndla.taxonomy.service.domain;


import javax.persistence.Entity;
import javax.persistence.OneToMany;
import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

@Entity
public class Subject extends DomainObject {

    public static final String LABEL = "subject";

    public Subject() {
        setPublicId(URI.create("urn:subject:" + UUID.randomUUID()));
    }

    public SubjectTopic addTopic(Topic topic) {
        Iterator<Topic> topics = getTopics();
        while (topics.hasNext()) {
            Topic t = topics.next();
            if (t.getId().equals(topic.getId()))
                throw new DuplicateIdException("Subject with id " + getPublicId() + " already contains topic with id " + topic.getPublicId());
        }

        SubjectTopic subjectTopic = new SubjectTopic(this, topic);
        subjectTopics.add(subjectTopic);
        topic.subjectTopics.add(subjectTopic);
        return subjectTopic;
    }

    @OneToMany(mappedBy = "subject")
    Set<SubjectTopic> subjectTopics = new HashSet<>();

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

    public Subject name(String name) {
        setName(name);
        return this;
    }
}