package no.ndla.taxonomy.service.domain;

import com.thinkaurelius.titan.core.TitanTransaction;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Edge;

public class TopicSubtopic extends DomainEdge {

    public static final String LABEL = "topic-has-subtopics";

    /**
     * Wraps an existing edge between a subject and a topic
     */
    public TopicSubtopic(Edge edge) {
        super(edge);
    }

    /**
     * Creates a new edge between a topic and a subtopic
     */
    public TopicSubtopic(Topic topic, Topic subtopic) {
        this(createEdge(topic, subtopic));
        setId("urn:topic-subtopic:" + edge.id());
    }

    private static Edge createEdge(Topic topic, Topic subtopic) {
        return topic.vertex.addEdge(TopicSubtopic.LABEL, subtopic.vertex);
    }

    public boolean isPrimary() {
         return is("primary");
    }

    public void setPrimary(boolean value) {
        setProperty("primary", value);
    }

    public Topic getTopic() {
        return new Topic(edge.outVertex());
    }

    public Topic getSubtopic() {
        return new Topic(edge.inVertex());
    }

    public static TopicSubtopic getById(String id, TitanTransaction transaction) {
        GraphTraversal<Edge, Edge> traversal = transaction.traversal().E().hasLabel(LABEL).has("id", id);
        if (traversal.hasNext()) return new TopicSubtopic(traversal.next());
        throw new NotFoundException("topic-subtopic", id);
    }
}
