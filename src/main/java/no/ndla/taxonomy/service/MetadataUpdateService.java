/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.service.dtos.MetadataDto;
import no.ndla.taxonomy.service.dtos.RecursiveMergeResultDto;

import java.net.URI;

public interface MetadataUpdateService {
    RecursiveMergeResultDto updateMetadataRecursivelyByPublicId(URI publicId, MetadataDto metadataApiEntity,
            boolean applyToResources);

    MetadataApiService getMetadataApiService();
}
