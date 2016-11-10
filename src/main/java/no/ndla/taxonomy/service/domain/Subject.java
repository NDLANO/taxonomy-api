package no.ndla.taxonomy.service.domain;

import com.thinkaurelius.titan.core.TitanTransaction;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.Iterator;

public class Subject extends DomainVertex {

    public static final String LABEL = "subject";

    public Subject(Vertex vertex) {
        super(vertex);
    }

    @Override
    protected String getLabel() {
        return LABEL;
    }

    public static Subject getById(Object id, TitanTransaction transaction) {
        GraphTraversal<Vertex, Vertex> traversal = transaction.traversal().V(id);
        if (traversal.hasNext()) return new Subject(traversal.next());
        throw new NotFoundException("subject", id);
    }

    public SubjectTopic addTopic(Topic topic) {
        Iterator<Edge> edges = vertex.edges(Direction.OUT, SubjectTopic.LABEL);
        if (edges.hasNext()) return new SubjectTopic(edges.next());

        Edge edge = vertex.addEdge(SubjectTopic.LABEL, topic.vertex);
        return new SubjectTopic(edge);
    }

    public Iterator<Topic> getTopics() {
        Iterator<Edge> edges = vertex.edges(Direction.OUT, SubjectTopic.LABEL);

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