package no.ndla.taxonomy.service.domain;

import com.thinkaurelius.titan.core.TitanTransaction;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.Iterator;

public class Subject extends DomainVertex {

    private static final String LABEL = "subject";

    public Subject(Vertex vertex) {
        super(vertex);
    }

    /**
     * Create a new subject
     *
     * @param transaction the transaction where the new vertex is created
     */
    public Subject(TitanTransaction transaction) {
        this(transaction.addVertex(LABEL));
        setId("urn:subject:" + vertex.id());
    }

    public Subject name(String name) {
        setName(name);
        return this;
    }

    public static Subject getById(String id, TitanTransaction transaction) {
        Subject subject = findById(id, transaction);
        if (subject != null) return subject;
        throw new NotFoundException("subject", id);
    }

    public static Subject findById(String id, TitanTransaction transaction) {
        GraphTraversal<Vertex, Vertex> traversal = transaction.traversal().V().has("id", id);
        return traversal.hasNext() ? new Subject(traversal.next()) : null;
    }

    public SubjectTopic addTopic(Topic topic) {
        return new SubjectTopic(this, topic);
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