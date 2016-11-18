package no.ndla.taxonomy.service.domain;

import com.thinkaurelius.titan.core.TitanTransaction;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Vertex;

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
     * @param transaction the transaction where the new vertex is created
     */
    public Topic(TitanTransaction transaction) {
        this(transaction.addVertex(LABEL));
        setId("urn:topic:" + vertex.id());
    }

    public static Topic getById(String id, TitanTransaction transaction) {
        GraphTraversal<Vertex, Vertex> traversal = transaction.traversal().V().has("id", id);
        if (traversal.hasNext()) return new Topic(traversal.next());
        throw new NotFoundException("topic", id);
    }

    public Topic name(String name) {
        setName(name);
        return this;
    }
}
