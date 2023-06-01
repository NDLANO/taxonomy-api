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
import java.util.HashMap;
import java.util.Map;
import no.ndla.taxonomy.domain.DomainEntity;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.exceptions.DuplicateIdException;
import no.ndla.taxonomy.repositories.TaxonomyRepository;
import no.ndla.taxonomy.service.ContextUpdaterService;
import no.ndla.taxonomy.service.URNValidator;
import no.ndla.taxonomy.service.UpdatableDto;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

public abstract class CrudController<T extends DomainEntity> {
    protected TaxonomyRepository<T> repository;
    protected ContextUpdaterService cachedUrlUpdaterService;

    private static final Map<Class<?>, String> locations = new HashMap<>();
    private final URNValidator validator = new URNValidator();

    protected CrudController(TaxonomyRepository<T> repository, ContextUpdaterService cachedUrlUpdaterService) {
        this.repository = repository;
        this.cachedUrlUpdaterService = cachedUrlUpdaterService;
    }

    protected CrudController(TaxonomyRepository<T> repository) {
        this.repository = repository;
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Deletes a single entity by id",
            security = {@SecurityRequirement(name = "oauth")})
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void deleteEntity(@PathVariable("id") URI id) {
        repository.delete(repository.getByPublicId(id));
        repository.flush();
    }

    @Operation(
            summary = "Updates a single entity by id",
            security = {@SecurityRequirement(name = "oauth")})
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    protected T updateEntity(URI id, UpdatableDto<T> command) {
        T entity = repository.getByPublicId(id);
        validator.validate(id, entity);
        command.apply(entity);

        if (entity instanceof Node && cachedUrlUpdaterService != null) {
            cachedUrlUpdaterService.updateContexts((Node) entity);
        }

        return entity;
    }

    @Operation(
            summary = "Creates a single entity",
            security = {@SecurityRequirement(name = "oauth")})
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    protected ResponseEntity<Void> createEntity(T entity, UpdatableDto<T> command) {
        try {
            command.getId().ifPresent(id -> {
                validator.validate(id, entity);
                entity.setPublicId(id);
            });

            command.apply(entity);
            URI location = URI.create(getLocation() + "/" + entity.getPublicId());
            repository.saveAndFlush(entity);

            if (entity instanceof Node && cachedUrlUpdaterService != null) {
                cachedUrlUpdaterService.updateContexts((Node) entity);
            }

            return ResponseEntity.created(location).build();
        } catch (DataIntegrityViolationException e) {
            command.getId().ifPresent(id -> {
                throw new DuplicateIdException(id.toString());
            });

            throw new DuplicateIdException();
        }
    }

    protected String getLocation() {
        return locations.computeIfAbsent(
                getClass(), aClass -> aClass.getAnnotation(RequestMapping.class).path()[0]);
    }
}
