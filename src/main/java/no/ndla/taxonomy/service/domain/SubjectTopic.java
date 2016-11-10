package no.ndla.taxonomy.service.domain;

import com.thinkaurelius.titan.core.TitanTransaction;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Edge;

public class SubjectTopic extends DomainEdge {

    public static final String LABEL = "subject-has-topics";

    public SubjectTopic(Edge edge) {
        super(edge);
    }

    @Override
    protected String getLabel() {
        return LABEL;
    }

    public boolean isPrimary() {
        return getProperty("primary");
    }

    public void setPrimary(boolean value) {
        setProperty("primary", value);
    }

    public Subject getSubject() {
        return new Subject(edge.outVertex());
    }

    public Topic getTopic() {
        return new Topic(edge.inVertex());
    }

    public static SubjectTopic getById(String id, TitanTransaction transaction) {
        GraphTraversal<Edge, Edge> traversal = transaction.traversal().E(id);
        if (traversal.hasNext()) return new SubjectTopic(traversal.next());
        throw new NotFoundException("subject-topic", id);
    }
}
