package no.ndla.taxonomy.service.domain;


import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;

import java.util.UUID;

public class SubjectTopic extends DomainEdge {

    public static final String LABEL = "subject-has-topics";

    /**
     * Wraps an existing edge between a subject and a topic
     */
    public SubjectTopic(Edge edge) {
        super(edge);
    }

    /**
     * Creates a new edge between a subject and a topic
     */
    public SubjectTopic(Subject subject, Topic topic) {
        this(createEdge(subject,topic));
        setId("urn:subject-topic:" + UUID.randomUUID());
    }

    private static Edge createEdge(Subject subject, Topic topic) {
        return null;// subject.vertex.addEdge(SubjectTopic.LABEL, topic.vertex);
    }

    public boolean isPrimary() {
         return is("primary");
    }

    public void setPrimary(boolean value) {
        setProperty("primary", value);
    }

    public Subject getSubject() {
        return null; //new Subject(edge.outVertex());
    }

    public Topic getTopic() {
        return new Topic(edge.inVertex());
    }

    public static SubjectTopic getById(String id, Graph graph) {
        GraphTraversal<Edge, Edge> traversal = graph.traversal().E().hasLabel(LABEL).has("id", id);
        if (traversal.hasNext()) return new SubjectTopic(traversal.next());
        throw new NotFoundException("subject-topic", id);
    }
}
