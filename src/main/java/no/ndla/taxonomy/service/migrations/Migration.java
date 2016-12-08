package no.ndla.taxonomy.service.migrations;

import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

public abstract class Migration {
    public static final String LABEL = "migration";
    private static final Logger log = LoggerFactory.getLogger(Migration.class);

    protected Graph graph;

    public final void run(Graph graph) {
        this.graph = graph;

        if (hasBeenRun()) {
            log.info(getName() + " has already been run, skipping.");
            return;
        }

        log.info("running " + getName());

        try {
            run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        rememberRun();
        log.debug("finished " + getName());
    }

    private void rememberRun() {
        Vertex vertex = graph.addVertex(LABEL);
        vertex.property("name", getName());
        vertex.property("timestamp", Instant.now());
    }

    private boolean hasBeenRun() {
        return graph.traversal().V().hasLabel(LABEL).has("name", getName()).hasNext();
    }

    public abstract void run() throws Exception;

    public String getName() {
        return getClass().getSimpleName();
    }
}
