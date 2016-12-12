package no.ndla.taxonomy.service;

import org.apache.tinkerpop.gremlin.structure.Graph;

public interface GraphFactory {
    Graph create();
    boolean isTest();
}
