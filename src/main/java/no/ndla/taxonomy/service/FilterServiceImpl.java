package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.DomainEntity;
import no.ndla.taxonomy.domain.exceptions.NotFoundException;
import no.ndla.taxonomy.repositories.FilterRepository;
import no.ndla.taxonomy.repositories.ResourceFilterRepository;
import no.ndla.taxonomy.repositories.ResourceRepository;
import no.ndla.taxonomy.repositories.SubjectRepository;
import no.ndla.taxonomy.service.dtos.FilterDTO;
import no.ndla.taxonomy.service.dtos.FilterWithConnectionDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Transactional(readOnly = true)
@Service
public class FilterServiceImpl implements FilterService {
    private final SubjectRepository subjectRepository;
    private final FilterRepository filterRepository;
    private final ResourceRepository resourceRepository;
    private final ResourceFilterRepository resourceFilterRepository;
    private final MetadataEntityWrapperService metadataEntityWrapperService;

    public FilterServiceImpl(SubjectRepository subjectRepository, FilterRepository filterRepository, ResourceRepository resourceRepository, ResourceFilterRepository resourceFilterRepository, MetadataEntityWrapperService metadataEntityWrapperService) {
        this.subjectRepository = subjectRepository;
        this.filterRepository = filterRepository;
        this.resourceRepository = resourceRepository;
        this.resourceFilterRepository = resourceFilterRepository;
        this.metadataEntityWrapperService = metadataEntityWrapperService;
    }

    @Override
    public List<FilterDTO> getFilters(String languageCode, boolean includeMetadata) {
        return metadataEntityWrapperService.wrapEntities(filterRepository.findAllWithSubjectAndTranslations(), includeMetadata, DomainEntity::getPublicId).stream()
                .map(wrappedFilter -> new FilterDTO(wrappedFilter, languageCode))
                .collect(Collectors.toList());
    }

    @Override
    public FilterDTO getFilterByPublicId(URI publicId, String languageCode, boolean includeMetadata) {
        return filterRepository.findFirstByPublicIdWithSubjectAndTranslations(publicId)
                .map(entity -> metadataEntityWrapperService.wrapEntity(entity, includeMetadata))
                .map((filter) -> new FilterDTO(filter, languageCode))
                .orElseThrow(() -> new NotFoundException("Filter", publicId));
    }

    @Override
    public List<FilterWithConnectionDTO> getFiltersWithConnectionByResourceId(URI resourceId, String languageCode, boolean includeMetadata) {
        if (!resourceRepository.existsByPublicId(resourceId)) {
            throw new NotFoundException("Resource", resourceId);
        }

        final var resourceFilterList = resourceFilterRepository.findAllByResourcePublicIdIncludingResourceAndFilterAndRelevance(resourceId);

        return metadataEntityWrapperService.wrapEntities(resourceFilterList, includeMetadata, entityConnection -> entityConnection.getFilter().getPublicId()).stream()
                .map(wrappedResourceFilter -> new FilterWithConnectionDTO(wrappedResourceFilter, languageCode))
                .collect(Collectors.toList());
    }

    @Override
    public List<FilterDTO> getFiltersBySubjectId(URI subjectId, String languageCode, boolean includeMetadata) {
        if (!subjectRepository.existsByPublicId(subjectId)) {
            throw new NotFoundException("Subject", subjectId);
        }

        final var filterList = new ArrayList<>(filterRepository.findAllBySubjectPublicIdIncludingSubjectAndTranslations(subjectId));
        return metadataEntityWrapperService.wrapEntities(filterList, includeMetadata)
                .stream()
                .map(filter -> new FilterDTO(filter, languageCode))
                .collect(Collectors.toList());
    }
}
