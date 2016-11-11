package no.ndla.taxonomy.service.domain;

import org.apache.tinkerpop.gremlin.structure.Element;

import java.net.URI;
import java.util.Iterator;

public abstract class DomainElement {

    private Element element;

    public DomainElement(Element element) {
        this.element = element;
    }

    protected <V> V getProperty(String property) {
        Iterator<V> name = element.values(property);
        return name.hasNext() ? name.next() : null;
    }

    protected <V> void setProperty(String property, V value) {
        element.property(property, value);
    }

    public URI getId() {
        String id = getProperty("id");
        if (id == null) return null;
        return URI.create(id);
    }

    public void setId(String id) {
        setId(URI.create(id));
    }

    public void setId(URI id) {
        setProperty("id", id.toString());
    }

    public void remove() {
        element.remove();
    }
}
