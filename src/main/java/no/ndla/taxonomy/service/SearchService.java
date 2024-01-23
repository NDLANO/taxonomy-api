/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import static org.springframework.data.jpa.domain.Specification.where;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import no.ndla.taxonomy.domain.DomainEntity;
import no.ndla.taxonomy.repositories.TaxonomyRepository;
import no.ndla.taxonomy.service.dtos.SearchResultDTO;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

interface ExtraSpecification<T> {
    Specification<T> applySpecification(Specification<T> spec);
}

public interface SearchService<DTO, DOMAIN extends DomainEntity, REPO extends TaxonomyRepository<DOMAIN>> {
    REPO getRepository();

    DTO createDTO(DOMAIN domain, String languageCode, Optional<Boolean> includeContexts, boolean filterProgrammes);

    private Specification<DOMAIN> base() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isNotNull(root.get("id"));
    }

    private Specification<DOMAIN> withNameLike(String name) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    private Specification<DOMAIN> withPublicIdsIn(List<URI> ids) {
        return (root, query, criteriaBuilder) -> root.get("publicId").in(ids);
    }

    private Specification<DOMAIN> withContentUriIn(List<URI> contentUris) {
        return (root, query, criteriaBuilder) -> root.get("contentUri").in(contentUris);
    }

    private Specification<DOMAIN> withKeyAndValue(String key, String value) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(
                criteriaBuilder.function(
                        "jsonb_extract_path_text",
                        String.class,
                        root.get("customfields"),
                        criteriaBuilder.literal(key)),
                value);
    }

    default SearchResultDTO<DTO> search(
            Optional<String> query,
            Optional<List<String>> ids,
            Optional<List<String>> contentUris,
            Optional<String> language,
            Optional<Boolean> includeContext,
            int pageSize,
            int page) {
        return search(
                query,
                ids,
                contentUris,
                language,
                includeContext,
                false,
                pageSize,
                page,
                Optional.empty(),
                Optional.empty());
    }

    default SearchResultDTO<DTO> search(
            Optional<String> query,
            Optional<List<String>> ids,
            Optional<List<String>> contentUris,
            Optional<String> language,
            Optional<Boolean> includeContexts,
            boolean filterProgrammes,
            int pageSize,
            int page,
            Optional<ExtraSpecification<DOMAIN>> applySpecLambda,
            Optional<Map<String, String>> customFieldFilters) {
        if (page < 1) throw new IllegalArgumentException("page parameter must be bigger than 0");

        var pageRequest = PageRequest.of(page - 1, pageSize);
        Specification<DOMAIN> spec = where(base());

        if (query.isPresent()) {
            spec = spec.and(withNameLike(query.get()));
        }

        spec = applyIdFilters(ids, spec);
        spec = applyContentUriFilters(contentUris, spec);

        if (applySpecLambda.isPresent()) {
            spec = applySpecLambda.get().applySpecification(spec);
        }

        spec = applyCustomFieldsFilters(spec, customFieldFilters);

        var fetched = getRepository().findAll(spec, pageRequest);

        var languageCode = language.orElse("");
        var dtos = fetched.stream()
                .map(r -> createDTO(r, languageCode, includeContexts, filterProgrammes))
                .collect(Collectors.toList());

        return new SearchResultDTO<>(fetched.getTotalElements(), page, pageSize, dtos);
    }

    private Specification<DOMAIN> applyContentUriFilters(
            Optional<List<String>> contentUris, Specification<DOMAIN> spec) {
        if (contentUris.isPresent()) {
            List<URI> urisToPass = contentUris.get().stream()
                    .flatMap(id -> {
                        try {
                            return Optional.of(new URI(id)).stream();
                        } catch (URISyntaxException ignored) {
                            /* ignore invalid urls sent by user */
                        }
                        return Optional.<URI>empty().stream();
                    })
                    .collect(Collectors.toList());

            if (!urisToPass.isEmpty()) spec = spec.and(withContentUriIn(urisToPass));
        }
        return spec;
    }

    private Specification<DOMAIN> applyIdFilters(Optional<List<String>> ids, Specification<DOMAIN> spec) {
        if (ids.isPresent()) {
            List<URI> urisToPass = ids.get().stream()
                    .flatMap(id -> {
                        try {
                            return Optional.of(new URI(id)).stream();
                        } catch (URISyntaxException ignored) {
                            /* ignore invalid urls sent by user */
                        }
                        return Optional.<URI>empty().stream();
                    })
                    .collect(Collectors.toList());

            if (!urisToPass.isEmpty()) spec = spec.and(withPublicIdsIn(urisToPass));
        }
        return spec;
    }

    private Specification<DOMAIN> applyCustomFieldsFilters(
            Specification<DOMAIN> spec, Optional<Map<String, String>> metadataFilters) {
        if (metadataFilters.isPresent()) {
            Specification<DOMAIN> filterSpec = null;
            for (var entry : metadataFilters.get().entrySet()) {
                if (filterSpec != null) {
                    filterSpec = filterSpec.or(withKeyAndValue(entry.getKey(), entry.getValue()));
                } else {
                    filterSpec = withKeyAndValue(entry.getKey(), entry.getValue());
                }
            }
            if (filterSpec != null) {
                return spec.and(filterSpec);
            }
        }
        return spec;
    }
}
