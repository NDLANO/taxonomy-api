package no.ndla.taxonomy.service.domain;

import org.apache.tinkerpop.gremlin.structure.Vertex;

public class Subject extends DomainObject {

    public Subject(Vertex vertex) {
        super(vertex);
    }
}