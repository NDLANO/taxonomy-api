/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import no.ndla.taxonomy.domain.ResourceType;
import no.ndla.taxonomy.domain.exceptions.NotFoundException;
import no.ndla.taxonomy.repositories.ResourceTypeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityManager;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(path = { "/v1/resource-types/{id}/translations" })
public class ResourceTypeTranslations {

    private final ResourceTypeRepository resourceTypeRepository;

    private final EntityManager entityManager;

    public ResourceTypeTranslations(ResourceTypeRepository resourceTypeRepository, EntityManager entityManager) {
        this.resourceTypeRepository = resourceTypeRepository;
        this.entityManager = entityManager;
    }

    @GetMapping
    @Operation(summary = "Gets all relevanceTranslations for a single resource type")
    @Transactional(readOnly = true)
    public List<ResourceTypeTranslationDTO> index(@PathVariable("id") URI id) {
        ResourceType resourceType = resourceTypeRepository.getByPublicId(id);
        List<ResourceTypeTranslationDTO> result = new ArrayList<>();
        resourceType.getTranslations().forEach(t -> result.add(new ResourceTypeTranslationDTO() {
            {
                name = t.getName();
                language = t.getLanguageCode();
            }
        }));
        return result;
    }

    @GetMapping("/{language}")
    @Operation(summary = "Gets a single translation for a single resource type")
    @Transactional(readOnly = true)
    public ResourceTypeTranslationDTO get(@PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb", required = true) @PathVariable("language") String language) {
        ResourceType resourceType = resourceTypeRepository.getByPublicId(id);
        var translation = resourceType.getTranslation(language).orElseThrow(
                () -> new NotFoundException("translation with language code " + language + " for resource type", id));

        return new ResourceTypeTranslationDTO() {
            {
                name = translation.getName();
                language = translation.getLanguageCode();
            }
        };
    }

    @PutMapping("/{language}")
    @Operation(summary = "Creates or updates a translation of a resource type", security = {
            @SecurityRequirement(name = "oauth") })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public void put(@PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb", required = true) @PathVariable("language") String language,
            @Parameter(name = "resourceType", description = "The new or updated translation") @RequestBody ResourceTypeTranslationPUT command) {
        ResourceType resourceType = resourceTypeRepository.getByPublicId(id);
        resourceType.addTranslation(command.name, language);
        entityManager.persist(resourceType);
    }

    @DeleteMapping("/{language}")
    @Operation(summary = "Deletes a translation", security = { @SecurityRequirement(name = "oauth") })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public void delete(@PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb", required = true) @PathVariable("language") String language) {
        ResourceType resourceType = resourceTypeRepository.getByPublicId(id);
        resourceType.getTranslation(language).ifPresent((translation) -> {
            resourceType.removeTranslation(language);
            entityManager.persist(resourceType);
        });
    }

    @Schema(name = "ResourceTypeTranslation")
    public static class ResourceTypeTranslationDTO {
        @JsonProperty
        @Schema(description = "The translated name of the resource type", example = "Article")
        public String name;

        @JsonProperty
        @Schema(description = "ISO 639-1 language code", example = "en")
        public String language;
    }

    public static class ResourceTypeTranslationPUT {
        @JsonProperty
        @Schema(description = "The translated name of the resource type", example = "Article")
        public String name;
    }
}
