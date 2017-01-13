package no.ndla.taxonomy.service.domain;


import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;

import java.util.UUID;

public class TopicResource extends DomainEdge {

    public static final String LABEL = "topic-has-resources";

    /**
     * Wraps an existing edge between a topic and a learning resource
     */
    public TopicResource(Edge edge) {
        super(edge);
    }

    /**
     * Creates a new edge between a topic and a resource
     */
    public TopicResource(Topic topic, Resource resource) {
        this(createEdge(topic, resource));
        setId("urn:topic-resource:" + UUID.randomUUID());
    }

    private static Edge createEdge(Topic topic, Resource resource) {
        return null; //topic.vertex.addEdge(TopicResource.LABEL, resource.vertex);
    }

    public boolean isPrimary() {
         return is("primary");
    }

    public void setPrimary(boolean value) {
        setProperty("primary", value);
    }

    public Topic getTopic() {
        return null; //new Topic(edge.outVertex());
    }

    public Topic getResource() {
        return null; //new Topic(edge.inVertex());
    }

    public static TopicResource getById(String id, Graph graph) {
        GraphTraversal<Edge, Edge> traversal = graph.traversal().E().hasLabel(LABEL).has("id", id);
        if (traversal.hasNext()) return new TopicResource(traversal.next());
        throw new NotFoundException("topic-resource", id);
    }
}
