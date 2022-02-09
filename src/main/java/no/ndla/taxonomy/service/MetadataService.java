/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Metadata;
import no.ndla.taxonomy.service.dtos.MetadataDto;
import no.ndla.taxonomy.service.exceptions.InvalidDataException;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MetadataService {

    MetadataDto getMetadataByPublicId(URI publicId);

    MetadataDto updateMetadataByPublicId(URI publicId, MetadataDto metadataDto) throws InvalidDataException;

    /*
     * Optional<Metadata> getMetadata(String publicId);
     *
     * List<Metadata> getMetadataList(Collection<String> publicIds);
     */

    /*
     * Metadata getOrCreateMetadata(String publicId);
     */

    /*
     * List<Metadata> getOrCreateMetadataList(Collection<String> publicId);
     */

    /*
     * Metadata saveMetadata(Metadata metadata);
     * 
     * void saveMetadataList(Collection<Metadata> metadataList);
     */
    /*
     * void deleteMetadata(String publicId);
     */
}
