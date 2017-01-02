package no.ndla.taxonomy.service.domain;

import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.UUID;

public class ResourceType extends DomainVertex {

    public static final String LABEL = "resource-type";

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

}
