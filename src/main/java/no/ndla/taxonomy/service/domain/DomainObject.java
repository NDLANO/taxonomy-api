package no.ndla.taxonomy.service.domain;

import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.Iterator;

public class DomainObject {
    protected Vertex vertex;

    public DomainObject(Vertex vertex) {
        this.vertex = vertex;
    }

    public Object getId() {
        return vertex.id();
    }

    public String getName() {
        return getProperty("name");
    }

    private <T> T getProperty(String property) {
        Iterator<T> name = vertex.values(property);
        return name.hasNext() ? name.next() : null;
    }

    public void setName(String name) {
        vertex.property("name", name);
    }

    public DomainObject name(String name) {
        setName(name);
        return this;
    }
}
