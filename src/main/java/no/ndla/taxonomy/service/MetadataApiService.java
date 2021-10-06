/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.service.dtos.MetadataDto;

import java.net.URI;
import java.util.Collection;
import java.util.Set;

public interface MetadataApiService {
    MetadataDto getMetadataByPublicId(URI publicId);

    Set<MetadataDto> getMetadataByKeyAndValue(String key, String value);

    Set<MetadataDto> getMetadataByPublicId(Collection<URI> publicIds);

    MetadataDto updateMetadataByPublicId(URI publicId, MetadataDto metadataDto);

    Set<MetadataDto> updateMetadataByPublicIds(Set<URI> publicIds, MetadataDto metaDataDto);

    void deleteMetadataByPublicId(URI publicId);
}
