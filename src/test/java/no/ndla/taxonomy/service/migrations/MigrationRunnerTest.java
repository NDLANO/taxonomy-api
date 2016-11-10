package no.ndla.taxonomy.service.migrations;

import ch.qos.logback.classic.Logger;
import com.thinkaurelius.titan.core.TitanFactory;
import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.TitanTransaction;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.InOrder;
import org.slf4j.LoggerFactory;

import static ch.qos.logback.classic.Level.INFO;
import static org.mockito.Mockito.*;

public class MigrationRunnerTest {
    private TitanGraph graph;
    private MigrationRunner migrationRunner;

    @BeforeClass
    public static void beforeClass() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(INFO);
    }

    @Before
    public void setUp() throws Exception {
        graph = TitanFactory.build().set("storage.backend", "inmemory").open();
        migrationRunner = new MigrationRunner();
    }

    @Test
    public void migrations_are_run_once() throws Exception {
        Migration migration1 = createMigration("Migration_1");
        Migration migration2 = createMigration("Migration_2");

        runMigrations();

        verify(migration1, times(1)).run();
        verify(migration2, times(1)).run();
    }

    @Test
    public void migrations_are_run_in_ascending_order_according_to_name() throws Exception {
        Migration migration3 = createMigration("Migration_2016_02_01T1200");
        Migration migration1 = createMigration("Migration_2016_01_01T1000");
        Migration migration2 = createMigration("Migration_2016_01_01T1200");

        runMigrations();

        InOrder order = inOrder(migration1, migration2, migration3);
        order.verify(migration1).run();
        order.verify(migration2).run();
        order.verify(migration3).run();
    }

    private Migration createMigration(String name) {
        Migration migration1 = mock(Migration.class);
        when(migration1.getName()).thenReturn(name);
        migrationRunner.addMigration(migration1);
        return migration1;
    }

    private void runMigrations() {
        try (TitanTransaction transaction = graph.newTransaction()) {
            migrationRunner.run(transaction);
        }
    }
}
