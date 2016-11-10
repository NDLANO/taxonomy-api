package no.ndla.taxonomy.service.domain;

import com.thinkaurelius.titan.core.TitanTransaction;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Vertex;

public class Topic extends DomainVertex {

    public static final String LABEL = "topic";

    public Topic(Vertex vertex) {
        super(vertex);
    }

    @Override
    protected String getLabel() {
        return LABEL;
    }

    public static Topic getById(Object id, TitanTransaction transaction) {
        GraphTraversal<Vertex, Vertex> traversal = transaction.traversal().V(id);
        if (traversal.hasNext()) return new Topic(traversal.next());
        throw new NotFoundException("topic", id);
    }
}
