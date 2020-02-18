package no.ndla.taxonomy.service;

import no.ndla.taxonomy.config.MetadataApiConfig;
import no.ndla.taxonomy.domain.MetadataApiEntity;
import no.ndla.taxonomy.service.dtos.MetadataDto;
import no.ndla.taxonomy.service.exceptions.ServiceUnavailableException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

@Component
public class MetadataApiServiceImpl implements MetadataApiService {
    private final RestTemplate restTemplate;
    private final String serviceUrl;

    public MetadataApiServiceImpl(MetadataApiConfig metadataApiConfig, RestTemplate restTemplate) {
        this.serviceUrl = metadataApiConfig.getServiceUrl();
        this.restTemplate = restTemplate;
    }

    @Override
    public MetadataDto getMetadataByPublicId(URI publicId) throws ServiceUnavailableException {
        if (serviceUrl == null) {
            throw new ServiceUnavailableException("No serviceUrl defined for taxonomy-metadata");
        }

        try {
            final var returnedEntity = restTemplate.getForEntity(serviceUrl + "/v1/taxonomy_entities/" + publicId, MetadataApiEntity.class).getBody();

            if (returnedEntity == null) {
                throw new ServiceUnavailableException("No response from service");
            }

            return new MetadataDto(returnedEntity);
        } catch (RestClientException exception) {
            throw new ServiceUnavailableException(exception);
        }
    }

    @Override
    public MetadataDto updateMetadataByPublicId(URI publicId, MetadataDto metadataApiEntity) throws ServiceUnavailableException {
        if (serviceUrl == null) {
            throw new ServiceUnavailableException("No serviceUrl defined for taxonomy-metadata");
        }

        final var requestObject = new MetadataApiEntity(metadataApiEntity);

        try {
            restTemplate.put(serviceUrl + "/v1/taxonomy_entities/" + publicId, requestObject);

            return getMetadataByPublicId(publicId);
        } catch (RestClientException exception) {
            throw new ServiceUnavailableException(exception);
        }
    }
}
