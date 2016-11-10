package no.ndla.taxonomy.service.domain;

import org.apache.tinkerpop.gremlin.structure.Vertex;

public abstract class DomainVertex extends DomainElement {
    protected final Vertex vertex;

    protected DomainVertex(Vertex vertex) {
        super(vertex);
        this.vertex = vertex;
    }

    public String getName() {
        return getProperty("name");
    }

    public void setName(String value) {
        setProperty("name", value);
    }

    public DomainVertex name(String name) {
        setName(name);
        return this;
    }
}
