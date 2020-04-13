package no.ndla.taxonomy.service;

import no.ndla.taxonomy.service.dtos.MetadataDto;

import java.net.URI;
import java.util.Collection;
import java.util.Set;

public interface MetadataApiService {
    MetadataDto getMetadataByPublicId(URI publicId);

    Set<MetadataDto> getMetadataByPublicId(Collection<URI> publicIds);

    MetadataDto updateMetadataByPublicId(URI publicId, MetadataDto metadataApiEntity);

    void deleteMetadataByPublicId(URI publicId);
}
