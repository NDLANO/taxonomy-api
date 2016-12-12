package no.ndla.taxonomy.service.domain;


import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.*;

import java.util.Iterator;
import java.util.UUID;

public class Subject extends DomainVertex {

    public static final String LABEL = "subject";

    public Subject(Vertex vertex) {
        super(vertex);
    }

    /**
     * Create a new subject
     *
     * @param graph the graph where the new vertex is created
     */
    public Subject(Graph graph) {
        this(graph.addVertex(LABEL));
        setId("urn:subject:" + UUID.randomUUID());
    }

    public Subject name(String name) {
        setName(name);
        return this;
    }

    public static Subject getById(String id, Graph graph) {
        Subject subject = findById(id, graph);
        if (subject != null) return subject;
        throw new NotFoundException("subject", id);
    }

    public static Subject findById(String id, Graph graph) {
        GraphTraversal<Vertex, Vertex> traversal = graph.traversal().V().hasLabel(LABEL).has("id", id);
        return traversal.hasNext() ? new Subject(traversal.next()) : null;
    }

    public SubjectTopic addTopic(Topic topic) {
        return new SubjectTopic(this, topic);
    }

    public Iterator<Topic> getTopics() {
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
    }
}