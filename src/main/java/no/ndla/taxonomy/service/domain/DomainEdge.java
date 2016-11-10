package no.ndla.taxonomy.service.domain;

import org.apache.tinkerpop.gremlin.structure.Edge;

public abstract class DomainEdge extends DomainElement {

    protected final Edge edge;

    public DomainEdge(Edge edge) {
        super(edge);
        this.edge = edge;
    }

}
