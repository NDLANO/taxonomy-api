package no.ndla.taxonomy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetadataApiConfig {
    private final String serviceUrl;

    public MetadataApiConfig(@Value("${ndla.taxonomy-metadata.url}") String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    public String getServiceUrl() {
        return serviceUrl;
    }
}
