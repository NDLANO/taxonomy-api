/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.net.URI;
import no.ndla.taxonomy.config.Constants;
import no.ndla.taxonomy.domain.DomainEntity;
import no.ndla.taxonomy.domain.EntityWithMetadata;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.exceptions.NotFoundException;
import no.ndla.taxonomy.repositories.TaxonomyRepository;
import no.ndla.taxonomy.service.ContextUpdaterService;
import no.ndla.taxonomy.service.dtos.MetadataDTO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

public abstract class CrudControllerWithMetadata<T extends DomainEntity> extends CrudController<T> {
    protected CrudControllerWithMetadata(
            TaxonomyRepository<T> repository, ContextUpdaterService contextUpdaterService) {
        super(repository, contextUpdaterService);
    }

    @GetMapping("/{id}/metadata")
    @Operation(summary = "Gets metadata for entity")
    public MetadataDTO getMetadata(@PathVariable("id") URI id) {
        var entity = repository.findByPublicId(id);
        if (entity instanceof EntityWithMetadata entityWithMetadata) {
            return new MetadataDTO(entityWithMetadata.getMetadata());
        }
        throw new NotFoundException("Entity", id);
    }

    @PutMapping(path = "/{id}/metadata")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Operation(
            summary = "Updates metadata for entity",
            security = {@SecurityRequirement(name = "oauth")})
    @Transactional
    public MetadataDTO putMetadata(@PathVariable("id") URI id, @RequestBody MetadataDTO entityToUpdate) {
        var entity = repository.findByPublicId(id);
        if (entity instanceof EntityWithMetadata entityWithMetadata) {
            entityToUpdate.setCustomField(Constants.IsChanged, Constants.True);
            contextUpdaterService.setCustomFieldOnParents(entityWithMetadata, Constants.ChildChanged, Constants.True);
            var result = entityWithMetadata.getMetadata().mergeWith(entityToUpdate);
            if (entity instanceof Node node) {
                contextUpdaterService.updateContexts(node);
            }
            return new MetadataDTO(result);
        }
        throw new NotFoundException("Entity", id);
    }
}
