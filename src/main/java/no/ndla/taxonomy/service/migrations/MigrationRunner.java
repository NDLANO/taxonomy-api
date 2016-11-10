package no.ndla.taxonomy.service.migrations;

import com.thinkaurelius.titan.core.TitanTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.util.ArrayList;
import java.util.List;

public class MigrationRunner {

    private String basePackage;
    private static final Logger log = LoggerFactory.getLogger(MigrationRunner.class);
    private List<Migration> migrations = new ArrayList<>();

    public MigrationRunner() {

    }

    public MigrationRunner(String basePackage) {
        this.basePackage = basePackage;
    }

    public void run(TitanTransaction transaction) {
        try {
            findMigrationClasses();

            migrations.sort((a, b) -> a.getName().compareTo(b.getName()));
            migrations.forEach(m -> m.run(transaction));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void findMigrationClasses() throws Exception {
        if (basePackage == null) {
            log.info("Base package not set, skipping class path scanning");
            return;
        }

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);

        scanner.addIncludeFilter(new AssignableTypeFilter(Migration.class));

        for (BeanDefinition bd : scanner.findCandidateComponents(basePackage)) {
            Class<?> migrationClass = Class.forName(bd.getBeanClassName());
            migrations.add((Migration) migrationClass.newInstance());
        }
    }

    public void addMigration(Migration migration) {
        migrations.add(migration);
    }
}
