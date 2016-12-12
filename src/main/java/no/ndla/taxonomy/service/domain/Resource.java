package no.ndla.taxonomy.service.domain;


import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.UUID;

public class Resource extends DomainVertex {

    public static final String LABEL = "resource";

    public Resource(Vertex vertex) {
        super(vertex);
    }

    /**
     * Create a new resource
     *
     * @param graph the graph where the new vertex is created
     */
    public Resource(Graph graph) {
        this(graph.addVertex(LABEL));
        setId("urn:resource:" + UUID.randomUUID());
    }

    public Resource name(String name) {
        setName(name);
        return this;
    }

    public static Resource getById(String id, Graph graph) {
        Resource resource = findById(id, graph);
        if (resource != null) return resource;
        throw new NotFoundException("resource", id);
    }

    public static Resource findById(String id, Graph graph) {
        GraphTraversal<Vertex, Vertex> traversal = graph.traversal().V().hasLabel(LABEL).has("id", id);
        return traversal.hasNext() ? new Resource(traversal.next()) : null;
    }
}