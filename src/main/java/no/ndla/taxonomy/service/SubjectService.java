/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.rest.v1.dtos.subjects.SubjectIndexDocument;

import java.net.URI;
import java.util.List;

public interface SubjectService {
    void delete(URI publicId);

    List<SubjectIndexDocument> getSubjects(String languageCode, MetadataKeyValueQuery metadataKeyValueQuery);

    List<SubjectIndexDocument> getSubjects(String languageCode);
}
