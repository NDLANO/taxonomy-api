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
import no.ndla.taxonomy.repositories.TaxonomyRepository;
import no.ndla.taxonomy.service.CachedUrlUpdaterService;
import no.ndla.taxonomy.service.MetadataService;
import no.ndla.taxonomy.service.dtos.MetadataDto;
import no.ndla.taxonomy.service.exceptions.InvalidDataException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.net.URI;

public abstract class CrudControllerWithMetadata<T extends DomainEntity> extends CrudController<T> {
    private final MetadataService metadataService;

    protected CrudControllerWithMetadata(TaxonomyRepository<T> repository,
            CachedUrlUpdaterService cachedUrlUpdaterService, MetadataService metadataService) {
        super(repository, cachedUrlUpdaterService);

        this.metadataService = metadataService;
    }

    @GetMapping("/{id}/metadata")
    @Operation(summary = "Gets metadata for entity")
    public MetadataDto getMetadata(@PathVariable("id") URI id) {
        return metadataService.getMetadataByPublicId(id);
    }

    @PutMapping(path = "/{id}/metadata")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Operation(summary = "Updates metadata for entity", security = { @SecurityRequirement(name = "oauth") })
    public MetadataDto putMetadata(@PathVariable("id") URI id, @RequestBody MetadataDto entityToUpdate)
            throws InvalidDataException {
        return metadataService.updateMetadataByPublicId(id, entityToUpdate);
    }
}
