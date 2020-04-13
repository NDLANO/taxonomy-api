package no.ndla.taxonomy.service;

import no.ndla.taxonomy.config.MetadataApiConfig;
import no.ndla.taxonomy.service.dtos.MetadataApiEntity;
import no.ndla.taxonomy.service.dtos.MetadataDto;
import no.ndla.taxonomy.service.exceptions.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
public class MetadataApiServiceImpl implements MetadataApiService {
    private final RestTemplate restTemplate;
    private final String serviceUrl;
    private final ThreadPoolExecutor executor;

    public MetadataApiServiceImpl(MetadataApiConfig metadataApiConfig, RestTemplate restTemplate,
                                  @Qualifier("metadataApiExecutor") ThreadPoolExecutor metadataApiExecutor) {
        this.serviceUrl = metadataApiConfig.getServiceUrl();
        this.restTemplate = restTemplate;
        this.executor = metadataApiExecutor;
    }

    private String getServiceUrl() {
        if (serviceUrl == null) {
            throw new ServiceUnavailableException("No serviceUrl defined for taxonomy-metadata");
        }

        return serviceUrl;
    }

    @Override
    public MetadataDto getMetadataByPublicId(URI publicId) {

        try {
            final var returnedEntity = restTemplate.getForEntity(getServiceUrl() + "/v1/taxonomy_entities/" + publicId, MetadataApiEntity.class).getBody();

            if (returnedEntity == null) {
                throw new ServiceUnavailableException("No response from service");
            }

            return new MetadataDto(returnedEntity);
        } catch (RestClientException exception) {
            throw new ServiceUnavailableException(exception);
        }
    }

    @Override
    public MetadataDto updateMetadataByPublicId(URI publicId, MetadataDto metadataApiEntity) {
        final var requestObject = new MetadataApiEntity(metadataApiEntity);

        try {
            restTemplate.put(getServiceUrl() + "/v1/taxonomy_entities/" + publicId, requestObject);

            return getMetadataByPublicId(publicId);
        } catch (RestClientException exception) {
            throw new ServiceUnavailableException(exception);
        }
    }

    @Override
    public void deleteMetadataByPublicId(URI publicId) throws ServiceUnavailableException {
        try {
            restTemplate.delete(getServiceUrl() + "/v1/taxonomy_entities/" + publicId);
        } catch (RestClientException exception) {
            throw new ServiceUnavailableException(exception);
        }
    }

    private Set<MetadataDto> doBulkRead(Collection<URI> publicIds) {
        if (publicIds.size() > 100) {
            throw new IllegalArgumentException("More than 100 entities in request");
        }

        final var publicIdCommaSeparatedList = String.join(",", publicIds.stream().map(URI::toString).collect(Collectors.toSet()));

        try {
            final var returnedEntities = restTemplate.getForEntity(getServiceUrl() + "/v1/taxonomy_entities/?publicIds=" + publicIdCommaSeparatedList, MetadataApiEntity[].class).getBody();

            if (returnedEntities == null) {
                throw new ServiceUnavailableException("No response from service");
            }

            return Arrays.stream(returnedEntities)
                    .map(MetadataDto::new)
                    .collect(Collectors.toSet());
        } catch (RestClientException exception) {
            throw new ServiceUnavailableException(exception);
        }

    }

    @Override
    public Set<MetadataDto> getMetadataByPublicId(Collection<URI> publicIds) {

        // Splits the collection of IDs into lists of maximum 100 entries each
        final var counter = new AtomicInteger(0);
        final var chunks = publicIds.stream()
                .collect(Collectors.groupingBy(iterator -> counter.getAndIncrement() / 100))
                .values();

        // Read each list in parallel from the metadata service
        final var metadataFutureEntries = chunks.stream()
                .map(list -> executor.submit(() -> doBulkRead(list)))
                .collect(Collectors.toSet());

        return metadataFutureEntries.stream()
                .map(futureSet -> {
                    try {
                        return futureSet.get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new ServiceUnavailableException(e.getMessage(), e);
                    }
                })
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }
}
