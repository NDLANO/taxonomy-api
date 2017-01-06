package no.ndla.taxonomy.service.domain;


import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.Iterator;
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
        GraphTraversal<Vertex, Vertex> traversal = graph.traversal().V().hasLabel(LABEL).has("id", id);
        if (traversal.hasNext()) {
            return new Resource(traversal.next());
        } else {
            throw new NotFoundException("resource", id);
        }
    }

    public Iterator<ResourceType> getResourceTypes() {
        Iterator<Edge> edges = vertex.edges(Direction.OUT, ResourceResourceType.LABEL);

        return new Iterator<ResourceType>() {
            @Override
            public boolean hasNext() {
                return edges.hasNext();
            }

            @Override
            public ResourceType next() {
                return new ResourceType(edges.next().inVertex());
            }
        };
    }

    public ResourceResourceType addResourceType(ResourceType resourceType) {
        return new ResourceResourceType(this, resourceType);
    }
}