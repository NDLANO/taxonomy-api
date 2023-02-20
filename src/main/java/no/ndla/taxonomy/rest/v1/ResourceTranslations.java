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
import no.ndla.taxonomy.domain.exceptions.NotFoundException;
import no.ndla.taxonomy.repositories.NodeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityManager;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(path = { "/v1/resources/{id}/translations" })
@Transactional(readOnly = true)
public class ResourceTranslations {

    private final NodeRepository nodeRepository;

    private final EntityManager entityManager;

    public ResourceTranslations(NodeRepository resourceRepository, EntityManager entityManager) {
        this.nodeRepository = resourceRepository;
        this.entityManager = entityManager;
    }

    @GetMapping
    @Operation(summary = "Gets all relevanceTranslations for a single resource")
    public List<ResourceTranslationIndexDocument> index(@PathVariable("id") URI id) {
        var resource = nodeRepository.getByPublicId(id);
        List<ResourceTranslationIndexDocument> result = new ArrayList<>();
        resource.getTranslations().forEach(t -> result.add(new ResourceTranslationIndexDocument() {
            {
                name = t.getName();
                language = t.getLanguageCode();
            }
        }));
        return result;
    }

    @GetMapping("/{language}")
    @Operation(summary = "Gets a single translation for a single resource")
    public ResourceTranslationIndexDocument get(@PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb", required = true) @PathVariable("language") String language) {
        var resource = nodeRepository.getByPublicId(id);
        var translation = resource.getTranslation(language).orElseThrow(
                () -> new NotFoundException("translation with language code " + language + " for resource", id));
        return new ResourceTranslationIndexDocument() {
            {
                name = translation.getName();
                language = translation.getLanguageCode();
            }
        };
    }

    @PutMapping("/{language}")
    @Operation(summary = "Creates or updates a translation of a resource", security = {
            @SecurityRequirement(name = "oauth") })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void put(@PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb", required = true) @PathVariable("language") String language,
            @Parameter(name = "resource", description = "The new or updated translation") @RequestBody UpdateResourceTranslationCommand command) {
        var resource = nodeRepository.getByPublicId(id);
        resource.addTranslation(command.name, language);
        entityManager.persist(resource);
    }

    @DeleteMapping("/{language}")
    @Operation(summary = "Deletes a translation", security = { @SecurityRequirement(name = "oauth") })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void delete(@PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb", required = true) @PathVariable("language") String language) {
        final var resource = nodeRepository.getByPublicId(id);
        resource.getTranslation(language).ifPresent((translation) -> {
            resource.removeTranslation(language);
            nodeRepository.save(resource);
        });
    }

    public static class ResourceTranslationIndexDocument {
        @JsonProperty
        @Schema(description = "The translated name of the resource", example = "Introduction to algebra")
        public String name;

        @JsonProperty
        @Schema(description = "ISO 639-1 language code", example = "en")
        public String language;
    }

    public static class UpdateResourceTranslationCommand {
        @JsonProperty
        @Schema(description = "The translated name of the resource", example = "Introduction to algebra")
        public String name;
    }
}
