package no.ndla.taxonomy.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;


@SpringBootApplication
@ImportResource("classpath:META-INF/applicationContext.xml")
public class TaxonomyApplication {

    public static void main(String[] args) throws InterruptedException {
        SpringApplication.run(TaxonomyApplication.class, args);
    }
}
