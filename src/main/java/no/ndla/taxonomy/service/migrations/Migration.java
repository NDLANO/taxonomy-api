package no.ndla.taxonomy.service.migrations;

import com.thinkaurelius.titan.core.TitanTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

public abstract class Migration {
    private static final String LABEL = "migration";
    private static final Logger log = LoggerFactory.getLogger(Migration.class);

    protected TitanTransaction transaction;

    public final void run(TitanTransaction transaction) {
        this.transaction = transaction;

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
        transaction.addVertex(LABEL).property("name", getName()).property("timestamp", Instant.now());
    }

    private boolean hasBeenRun() {
        return transaction.traversal().V().hasLabel(LABEL).has("name", getName()).hasNext();
    }

    public abstract void run() throws Exception;

    public String getName() {
        return getClass().getSimpleName();
    }
}
