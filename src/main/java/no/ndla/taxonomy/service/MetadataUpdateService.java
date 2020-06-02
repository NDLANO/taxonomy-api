package no.ndla.taxonomy.service;

import no.ndla.taxonomy.service.dtos.MetadataDto;
import no.ndla.taxonomy.service.dtos.RecursiveMergeResultDto;

import java.net.URI;

public interface MetadataUpdateService {
    RecursiveMergeResultDto updateMetadataRecursivelyByPublicId(URI publicId, MetadataDto metadataApiEntity, boolean applyToResources);

    MetadataApiService getMetadataApiService();
}
