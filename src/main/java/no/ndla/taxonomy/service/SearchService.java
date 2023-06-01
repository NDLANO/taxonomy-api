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

    DTO createDTO(DOMAIN domain, String languageCode, Optional<Boolean> includeContexts);

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

    default SearchResultDTO<DTO> search(
            Optional<String> query,
            Optional<List<String>> ids,
            Optional<String> language,
            Optional<Boolean> includeContext,
            int pageSize,
            int page) {
        return search(query, ids, language, includeContext, pageSize, page, Optional.empty());
    }

    default SearchResultDTO<DTO> search(
            Optional<String> query,
            Optional<List<String>> ids,
            Optional<String> language,
            Optional<Boolean> includeContexts,
            int pageSize,
            int page,
            Optional<ExtraSpecification<DOMAIN>> applySpecLambda) {
        if (page < 1) throw new IllegalArgumentException("page parameter must be bigger than 0");

        var pageRequest = PageRequest.of(page - 1, pageSize);
        Specification<DOMAIN> spec = where(base());

        if (query.isPresent()) {
            spec = spec.and(withNameLike(query.get()));
        }

        if (ids.isPresent()) {
            List<URI> urisToPass = ids.get().stream()
                    .flatMap(id -> {
                        try {
                            return Optional.of(new URI(id)).stream();
                        } catch (URISyntaxException ignored) {
                            /* ignore invalid urls sent by user */ }
                        return Optional.<URI>empty().stream();
                    })
                    .collect(Collectors.toList());

            if (!urisToPass.isEmpty()) spec = spec.and(withPublicIdsIn(urisToPass));
        }

        if (applySpecLambda.isPresent()) {
            spec = applySpecLambda.get().applySpecification(spec);
        }

        var fetched = getRepository().findAll(spec, pageRequest);

        var languageCode = language.orElse("");
        var dtos = fetched.stream()
                .map(r -> createDTO(r, languageCode, includeContexts))
                .collect(Collectors.toList());

        return new SearchResultDTO<>(fetched.getTotalElements(), page, pageSize, dtos);
    }
}
