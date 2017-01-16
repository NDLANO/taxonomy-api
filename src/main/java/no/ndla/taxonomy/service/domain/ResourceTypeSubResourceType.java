package no.ndla.taxonomy.service.domain;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;

import java.util.UUID;

public class ResourceTypeSubResourceType extends DomainEdge {

    public static final String LABEL = "resourcetype-has-subresourcetypes";

    /**
     * Wraps an existing edge between a resource type and a subresource type
     */
    public ResourceTypeSubResourceType(Edge edge) {
        super(edge);
    }

    /**
     * Creates a new edge between a resource type and a subresource type
     */
    public ResourceTypeSubResourceType(ResourceType resourceType, ResourceType subresourceType) {
        this(createEdge(resourceType, subresourceType));
        setId("urn:resourcetype-subresourcetype:" + UUID.randomUUID());
    }

    private static Edge createEdge(ResourceType resourceType, ResourceType subresourceType) {
        return resourceType.vertex.addEdge(ResourceTypeSubResourceType.LABEL, subresourceType.vertex);
    }

    public ResourceType getParentResourcetype() {
        return new ResourceType(edge.outVertex());
    }

    public ResourceType getSubResourceType() {
        return new ResourceType(edge.inVertex());
    }


    public static ResourceTypeSubResourceType getById(String id, Graph graph) {
        GraphTraversal<Edge, Edge> traversal = graph.traversal().E().hasLabel(LABEL).has("id", id);
        if (traversal.hasNext()) return new ResourceTypeSubResourceType(traversal.next());
        throw new NotFoundException("resourcetype-subresourcetype", id);
    }
}
