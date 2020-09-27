package no.ndla.taxonomy.service;

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
import java.util.List;
import java.util.stream.Collectors;

@Transactional(readOnly = true)
@Service
public class FilterServiceImpl implements FilterService {
    private final SubjectRepository subjectRepository;
    private final FilterRepository filterRepository;
    private final ResourceRepository resourceRepository;
    private final ResourceFilterRepository resourceFilterRepository;

    public FilterServiceImpl(SubjectRepository subjectRepository, FilterRepository filterRepository, ResourceRepository resourceRepository, ResourceFilterRepository resourceFilterRepository) {
        this.subjectRepository = subjectRepository;
        this.filterRepository = filterRepository;
        this.resourceRepository = resourceRepository;
        this.resourceFilterRepository = resourceFilterRepository;
    }

    @Override
    @InjectMetadata
    public List<FilterDTO> getFilters(String languageCode) {
        return filterRepository.findAllWithSubjectAndTranslations().stream()
                .map(wrappedFilter -> new FilterDTO(wrappedFilter, languageCode))
                .collect(Collectors.toList());
    }

    @Override
    @InjectMetadata
    public FilterDTO getFilterByPublicId(URI publicId, String languageCode) {
        return filterRepository.findFirstByPublicIdWithSubjectAndTranslations(publicId)
                .map((filter) -> new FilterDTO(filter, languageCode))
                .orElseThrow(() -> new NotFoundException("Filter", publicId));
    }

    @Override
    @InjectMetadata
    public List<FilterWithConnectionDTO> getFiltersWithConnectionByResourceId(URI resourceId, String languageCode) {
        if (!resourceRepository.existsByPublicId(resourceId)) {
            throw new NotFoundException("Resource", resourceId);
        }

        return resourceFilterRepository.findAllByResourcePublicIdIncludingResourceAndFilterAndRelevance(resourceId)
                .stream()
                .map(wrappedResourceFilter -> new FilterWithConnectionDTO(wrappedResourceFilter, languageCode))
                .collect(Collectors.toList());
    }

    @Override
    @InjectMetadata
    public List<FilterDTO> getFiltersBySubjectId(URI subjectId, String languageCode) {
        if (!subjectRepository.existsByPublicId(subjectId)) {
            throw new NotFoundException("Subject", subjectId);
        }

        return filterRepository.findAllBySubjectPublicIdIncludingSubjectAndTranslations(subjectId)
                .stream()
                .map(filter -> new FilterDTO(filter, languageCode))
                .collect(Collectors.toList());
    }
}
