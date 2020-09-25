package no.ndla.taxonomy.service;

import no.ndla.taxonomy.service.dtos.FilterDTO;
import no.ndla.taxonomy.service.dtos.FilterWithConnectionDTO;

import java.net.URI;
import java.util.List;

public interface FilterService {
    List<FilterDTO> getFilters(String languageCode, boolean includeMetadata);

    List<FilterDTO> getFiltersBySubjectId(URI subjectId, String languageCode, boolean includeMetadata);

    FilterDTO getFilterByPublicId(URI publicId, String languageCode, boolean includeMetadata);

    List<FilterWithConnectionDTO> getFiltersWithConnectionByResourceId(URI resourceId, String languageCode, boolean includeMetadata);
}
