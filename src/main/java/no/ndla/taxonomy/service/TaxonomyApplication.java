package no.ndla.taxonomy.service;

import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.TitanTransaction;
import no.ndla.taxonomy.service.migrations.MigrationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import static org.springframework.boot.SpringApplication.run;


@SpringBootApplication
public class TaxonomyApplication {

    public static void main(String[] args) throws InterruptedException {
        run(TaxonomyApplication.class, args);
    }


}
