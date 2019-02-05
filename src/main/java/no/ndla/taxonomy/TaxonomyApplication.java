package no.ndla.taxonomy;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import static org.springframework.boot.SpringApplication.run;

@SpringBootApplication
@EnableScheduling
public class TaxonomyApplication {
    public static void main(String[] args) throws InterruptedException {
        run(TaxonomyApplication.class, args);
    }
}
