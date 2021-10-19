/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.ResourceType;
import no.ndla.taxonomy.domain.ResourceTypeTranslation;
import no.ndla.taxonomy.domain.exceptions.NotFoundException;
import no.ndla.taxonomy.repositories.ResourceTypeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(path = { "/v1/resource-types/{id}/translations" })
@Transactional
public class ResourceTypeTranslations {

    private final ResourceTypeRepository resourceTypeRepository;

    private final EntityManager entityManager;

    public ResourceTypeTranslations(ResourceTypeRepository resourceTypeRepository, EntityManager entityManager) {
        this.resourceTypeRepository = resourceTypeRepository;
        this.entityManager = entityManager;
    }

    @GetMapping
    @ApiOperation("Gets all relevanceTranslations for a single resource type")
    public List<ResourceTypeTranslations.ResourceTypeTranslationIndexDocument> index(@PathVariable("id") URI id) {
        ResourceType resourceType = resourceTypeRepository.getByPublicId(id);
        List<ResourceTypeTranslations.ResourceTypeTranslationIndexDocument> result = new ArrayList<>();
        resourceType.getTranslations()
                .forEach(t -> result.add(new ResourceTypeTranslations.ResourceTypeTranslationIndexDocument() {
                    {
                        name = t.getName();
                        language = t.getLanguageCode();
                    }
                }));
        return result;
    }

    @GetMapping("/{language}")
    @ApiOperation("Gets a single translation for a single resource type")
    public ResourceTypeTranslations.ResourceTypeTranslationIndexDocument get(@PathVariable("id") URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb", required = true) @PathVariable("language") String language) {
        ResourceType resourceType = resourceTypeRepository.getByPublicId(id);
        ResourceTypeTranslation translation = resourceType.getTranslation(language).orElseThrow(
                () -> new NotFoundException("translation with language code " + language + " for resource type", id));

        return new ResourceTypeTranslations.ResourceTypeTranslationIndexDocument() {
            {
                name = translation.getName();
                language = translation.getLanguageCode();
            }
        };
    }

    @PutMapping("/{language}")
    @ApiOperation("Creates or updates a translation of a resource type")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void put(@PathVariable("id") URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb", required = true) @PathVariable("language") String language,
            @ApiParam(name = "resourceType", value = "The new or updated translation") @RequestBody ResourceTypeTranslations.UpdateResourceTypeTranslationCommand command) {
        ResourceType resourceType = resourceTypeRepository.getByPublicId(id);
        ResourceTypeTranslation translation = resourceType.addTranslation(language);
        entityManager.persist(translation);
        translation.setName(command.name);
    }

    @DeleteMapping("/{language}")
    @ApiOperation("Deletes a translation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void delete(@PathVariable("id") URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb", required = true) @PathVariable("language") String language) {
        ResourceType resourceType = resourceTypeRepository.getByPublicId(id);
        resourceType.getTranslation(language).ifPresent((translation) -> {
            resourceType.removeTranslation(language);
            entityManager.remove(translation);
        });
    }

    public static class ResourceTypeTranslationIndexDocument {
        @JsonProperty
        @ApiModelProperty(value = "The translated name of the resource type", example = "Article")
        public String name;

        @JsonProperty
        @ApiModelProperty(value = "ISO 639-1 language code", example = "en")
        public String language;
    }

    public static class UpdateResourceTypeTranslationCommand {
        @JsonProperty
        @ApiModelProperty(value = "The translated name of the resource type", example = "Article")
        public String name;
    }
}
