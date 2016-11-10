package no.ndla.taxonomy.service;

import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.TitanTransaction;
import no.ndla.taxonomy.service.migrations.MigrationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;

import static org.springframework.boot.SpringApplication.run;


@SpringBootApplication
@ImportResource("classpath:META-INF/applicationContext.xml")
public class TaxonomyApplication {

    public static void main(String[] args) throws InterruptedException {
        run(TaxonomyApplication.class, args);
    }

    @Bean
    public String runMigrations(TitanGraph graph) {
        try (TitanTransaction transaction = graph.newTransaction()) {
            new MigrationRunner("no.ndla.taxonomy.service.migrations").run(transaction);
            transaction.commit();
        }
        return "";
    }
}
