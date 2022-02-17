/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.service.dtos.MetadataDto;
import no.ndla.taxonomy.service.exceptions.InvalidDataException;

import java.net.URI;

public interface MetadataService {
    MetadataDto getMetadataByPublicId(URI publicId);

    MetadataDto updateMetadataByPublicId(URI publicId, MetadataDto metadataDto) throws InvalidDataException;
}
