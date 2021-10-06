/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class MetadataApiConfig {
    private final String serviceUrl;
    private final int clientPoolSize;

    public MetadataApiConfig(@Value("${ndla.taxonomy-metadata.url}") String serviceUrl,
                             @Value("${ndla.taxonomy-metadata.client-pool-size:10}") int clientPoolSize) {
        this.serviceUrl = serviceUrl;
        this.clientPoolSize = clientPoolSize;
    }

    public String getServiceUrl() {
        return serviceUrl;
    }

    @Bean(name = "metadataApiExecutor")
    public ThreadPoolExecutor getMetadataApiExecutor() {
        return (ThreadPoolExecutor) Executors.newFixedThreadPool(clientPoolSize);
    }
}
