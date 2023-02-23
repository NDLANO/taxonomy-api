/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import no.ndla.taxonomy.domain.DomainEntity;
import no.ndla.taxonomy.domain.EntityWithMetadata;
import no.ndla.taxonomy.domain.Metadata;
import no.ndla.taxonomy.domain.exceptions.NotFoundException;
import no.ndla.taxonomy.repositories.TaxonomyRepository;
import no.ndla.taxonomy.service.CachedUrlUpdaterService;
import no.ndla.taxonomy.service.DomainEntityHelperService;
import no.ndla.taxonomy.service.dtos.MetadataDto;
import no.ndla.taxonomy.service.exceptions.InvalidDataException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.net.URI;

public abstract class CrudControllerWithMetadata<T extends DomainEntity> extends CrudController<T> {
    protected CrudControllerWithMetadata(TaxonomyRepository<T> repository,
            CachedUrlUpdaterService cachedUrlUpdaterService) {
        super(repository, cachedUrlUpdaterService);
    }

    @GetMapping("/{id}/metadata")
    @Operation(summary = "Gets metadata for entity")
    public MetadataDto getMetadata(@PathVariable("id") URI id) {
        var entity = repository.findByPublicId(id);
        if (entity instanceof EntityWithMetadata em) {
            return new MetadataDto(em.getMetadata());
        }
        throw new NotFoundException("Entity", id);
    }

    @PutMapping(path = "/{id}/metadata")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Operation(summary = "Updates metadata for entity", security = { @SecurityRequirement(name = "oauth") })
    @Transactional
    public MetadataDto putMetadata(@PathVariable("id") URI id, @RequestBody MetadataDto entityToUpdate)
            throws InvalidDataException {
        var entity = repository.findByPublicId(id);
        if (entity instanceof EntityWithMetadata em) {
            var result = em.getMetadata().mergeWith(entityToUpdate);
            return new MetadataDto(result);
        }
        throw new NotFoundException("Entity", id);
    }
}
