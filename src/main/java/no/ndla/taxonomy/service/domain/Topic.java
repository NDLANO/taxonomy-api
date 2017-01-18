package no.ndla.taxonomy.service.domain;


import javax.persistence.Entity;
import javax.persistence.OneToMany;
import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

@Entity
public class Topic extends DomainObject {

    public static final String LABEL = "topic";

    @OneToMany(mappedBy = "topic")
    Set<SubjectTopic> subjectTopics = new HashSet<>();

    @OneToMany(mappedBy = "topic")
    private Set<TopicSubtopic> topicSubtopics = new HashSet<>();

    public Topic() {
        setPublicId(URI.create("urn:topic:" + UUID.randomUUID()));
    }

    public Topic name(String name) {
        setName(name);
        return this;
    }

    public TopicSubtopic addSubtopic(Topic subtopic) {
        Iterator<Topic> topics = getSubtopics();
        while (topics.hasNext()) {
            Topic t = topics.next();
            if (t.getId().equals(subtopic.getId())) {
                throw new DuplicateIdException("Topic with id " + getPublicId() + " already contains topic with id " + subtopic.getPublicId());
            }
        }

        TopicSubtopic topicSubtopic = new TopicSubtopic(this, subtopic);
        topicSubtopics.add(topicSubtopic);
        return topicSubtopic;
    }

    public TopicResource addResource(Resource resource) {
        return new TopicResource(this, resource);
    }

    public Iterator<Topic> getSubtopics() {
        Iterator<TopicSubtopic> iterator = topicSubtopics.iterator();

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


    public Iterator<Resource> getResources() {
        return null; /*
        Iterator<Edge> edges = vertex.edges(Direction.OUT, TopicResource.LABEL);

        return new Iterator<Resource>() {
            @Override
            public boolean hasNext() {
                return edges.hasNext();
            }

            @Override
            public Resource next() {
                return new Resource(edges.next().inVertex());
            }
        };*/
    }

}
