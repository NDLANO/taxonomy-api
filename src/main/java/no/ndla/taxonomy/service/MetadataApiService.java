package no.ndla.taxonomy.service;

import no.ndla.taxonomy.service.dtos.MetadataDto;
import no.ndla.taxonomy.service.exceptions.ServiceUnavailableException;

import java.net.URI;

public interface MetadataApiService {
    MetadataDto getMetadataByPublicId(URI publicId) throws ServiceUnavailableException;

    MetadataDto updateMetadataByPublicId(URI publicId, MetadataDto metadataApiEntity) throws ServiceUnavailableException;
}
