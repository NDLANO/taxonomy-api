/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.service.dtos.SubjectDTO;

import java.net.URI;
import java.util.List;

public interface SubjectService {
    void delete(URI publicId);

    List<SubjectDTO> getSubjects(String languageCode, MetadataKeyValueQuery metadataKeyValueQuery);

    List<SubjectDTO> getSubjects(String languageCode);
}
