package no.ndla.taxonomy.service.domain;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.Iterator;
import java.util.UUID;

public class ResourceType extends DomainVertex {

    public static final String LABEL = "resource-type";

    /**
     * Wrap an existing resource type
     *
     * @param vertex the vertex to wrap
     */
    public ResourceType(Vertex vertex) {
        super(vertex);
    }

    /**
     * Create a new resource type
     *
     * @param graph the graph where the new vertex is created
     */
    public ResourceType(Graph graph) {

        this(graph.addVertex(LABEL));
        setId("urn:resource-type:" + UUID.randomUUID());
    }


    public ResourceType name(String name) {
        setName(name);
        return this;
    }

    public static ResourceType getById(String id, Graph graph) {
        GraphTraversal<Vertex, Vertex> traversal = graph.traversal().V().hasLabel(LABEL).has("id", id);
        if (traversal.hasNext()) {
            return new ResourceType(traversal.next());
        } else {
            throw new NotFoundException("resource-type", id);
        }
    }

    public ResourceTypeSubResourceType addParentResourceType(ResourceType parentResourceType) {
        return new ResourceTypeSubResourceType(parentResourceType, this);
    }

    public void removeParentResourceType() {
        final Iterator<Edge> edges = vertex.edges(Direction.IN, ResourceTypeSubResourceType.LABEL);
        if (edges.hasNext()) {
            edges.next().remove();
        }
    }

    public ResourceType getParent() {
        final Iterator<Edge> edges = vertex.edges(Direction.IN, ResourceTypeSubResourceType.LABEL);
        if (edges.hasNext()) {
            return new ResourceType(edges.next().outVertex());
        }
        throw new NotFoundException("Parent resource type for resource type", this.getId());
    }

        public Iterator<ResourceType> getSubResourceTypes () {
            Iterator<Edge> edges = vertex.edges(Direction.OUT, ResourceTypeSubResourceType.LABEL);

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
}
