package no.ndla.taxonomy.service.domain;


import javax.persistence.Entity;
import java.net.URI;
import java.util.Iterator;
import java.util.UUID;

@Entity(name = "subjects")
public class Subject extends DomainObject {

    public static final String LABEL = "subject";

    public Subject() {
        setPublicId(URI.create("urn:subject:" + UUID.randomUUID()));
    }

    public SubjectTopic addTopic(Topic topic) {
        return new SubjectTopic(this, topic);
    }

    public Iterator<Topic> getTopics() {
        return null;

        /*
        Iterator<Edge> edges = vertex.edges(Direction.OUT, SubjectTopic.LABEL);

        return new Iterator<Topic>() {
            @Override
            public boolean hasNext() {
                return edges.hasNext();
            }

            @Override
            public Topic next() {
                return new Topic(edges.next().inVertex());
            }
        };

        */
    }

    public Subject name(String name) {
        setName(name);
        return this;
    }
}