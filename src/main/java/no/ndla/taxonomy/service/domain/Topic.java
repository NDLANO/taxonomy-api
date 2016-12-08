package no.ndla.taxonomy.service.domain;


import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.Iterator;
import java.util.UUID;

public class Topic extends DomainVertex {

    public static final String LABEL = "topic";

    /**
     * Wrap an existing topic
     *
     * @param vertex the vertex to wrap
     */
    public Topic(Vertex vertex) {
        super(vertex);
    }

    /**
     * Create a new topic
     *
     * @param graph the graph where the new vertex is created
     */
    public Topic(Graph graph) {
        this(graph.addVertex(LABEL));
        setId("urn:topic:" + UUID.randomUUID());
    }

    public static Topic getById(String id, Graph graph) {
        Topic topic = findById(id, graph);
        if (topic != null) return topic;
        throw new NotFoundException("topic", id);
    }

    public static Topic findById(String id, Graph graph) {
        GraphTraversal<Vertex, Vertex> traversal = graph.traversal().V().hasLabel(LABEL).has("id", id);
        return traversal.hasNext() ? new Topic(traversal.next()) : null;
    }

    public Topic name(String name) {
        setName(name);
        return this;
    }

    public TopicSubtopic addSubtopic(Topic topic) {
        return new TopicSubtopic(this, topic);
    }

    public Iterator<Topic> getSubtopics() {
        Iterator<Edge> edges = vertex.edges(Direction.OUT, TopicSubtopic.LABEL);

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
