/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import no.ndla.taxonomy.domain.JsonTranslation;
import no.ndla.taxonomy.domain.ResourceType;
import no.ndla.taxonomy.domain.exceptions.NotFoundException;
import no.ndla.taxonomy.repositories.ResourceTypeRepository;
import no.ndla.taxonomy.rest.v1.NodeTranslations.TranslationDTO;
import no.ndla.taxonomy.service.UpdatableDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = { "/v1/resource-types" })
public class ResourceTypes extends CrudController<ResourceType> {

    private final ResourceTypeRepository resourceTypeRepository;

    public ResourceTypes(ResourceTypeRepository resourceTypeRepository) {
        super(resourceTypeRepository);

        this.resourceTypeRepository = resourceTypeRepository;
    }

    @GetMapping
    @Operation(summary = "Gets a list of all resource types")
    public List<ResourceTypeIndexDocument> index(
            @Parameter(description = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", required = false, defaultValue = "") String language) {
        // Returns all resource types that is NOT a subtype
        return resourceTypeRepository.findAllByParentIncludingTranslationsAndFirstLevelSubtypes(null).stream()
                .map(resourceType -> new ResourceTypeIndexDocument(resourceType, language, 100))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Gets a single resource type")
    public ResourceTypeIndexDocument get(@PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", required = false, defaultValue = "") String language) {
        return resourceTypeRepository.findFirstByPublicIdIncludingTranslations(id)
                .map(resourceType -> new ResourceTypeIndexDocument(resourceType, language, 0))
                .orElseThrow(() -> new NotFoundException("ResourceType", id));
    }

    @PostMapping
    @Operation(summary = "Adds a new resource type", security = { @SecurityRequirement(name = "oauth") })
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public ResponseEntity<Void> post(
            @Parameter(name = "resourceType", description = "The new resource type") @RequestBody ResourceTypeCommand command) {
        ResourceType resourceType = new ResourceType();
        if (null != command.parentId) {
            ResourceType parent = resourceTypeRepository.getByPublicId(command.parentId);
            resourceType.setParent(parent);
        }
        return doPost(resourceType, command);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Updates a resource type. Use to update which resource type is parent. You can also update the id, take care!", security = {
            @SecurityRequirement(name = "oauth") })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void put(@PathVariable URI id,
            @Parameter(name = "resourceType", description = "The updated resource type. Fields not included will be set to null.") @RequestBody ResourceTypeCommand command) {
        ResourceType resourceType = doPut(id, command);

        ResourceType parent = null;
        if (command.parentId != null) {
            parent = resourceTypeRepository.getByPublicId(command.parentId);
        }
        resourceType.setParent(parent);
        if (command.id != null) {
            resourceType.setPublicId(command.id);
        }
    }

    @GetMapping("/{id}/subtypes")
    @Operation(summary = "Gets subtypes of one resource type")
    public List<ResourceTypeIndexDocument> getSubtypes(@PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", required = false, defaultValue = "") String language,
            @RequestParam(value = "recursive", required = false, defaultValue = "false") @Parameter(description = "If true, sub resource types are fetched recursively") boolean recursive) {
        return resourceTypeRepository.findAllByParentPublicIdIncludingTranslationsAndFirstLevelSubtypes(id).stream()
                .map(resourceType -> new ResourceTypeIndexDocument(resourceType, language, 100))
                .collect(Collectors.toList());
    }

    @Schema(name = "ResourceTypeIndexDocument")
    public static class ResourceTypeIndexDocument {
        @JsonProperty
        @Schema(example = "urn:resourcetype:1")
        public URI id;

        @JsonProperty
        @Schema(description = "The name of the resource type", example = "Lecture")
        public String name;

        @JsonProperty
        @Schema(description = "Sub resource types")
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public List<ResourceTypeIndexDocument> subtypes = new ArrayList<>();

        @JsonProperty
        @Schema(description = "All translations of this resource type")
        private Set<TranslationDTO> translations;

        @JsonProperty
        @Schema(description = "List of language codes supported by translations")
        private Set<String> supportedLanguages;

        public ResourceTypeIndexDocument() {
        }

        public ResourceTypeIndexDocument(ResourceType resourceType, String language, int recursionLevels) {
            this.id = resourceType.getPublicId();

            var translations = resourceType.getTranslations();
            this.translations = translations.stream().map(TranslationDTO::new).collect(Collectors.toSet());
            this.supportedLanguages = this.translations.stream().map(t -> t.language).collect(Collectors.toSet());

            this.name = resourceType.getTranslation(language).map(JsonTranslation::getName)
                    .orElse(resourceType.getName());

            if (recursionLevels > 0) {
                this.subtypes = resourceType.getSubtypes().stream().map(
                        resourceType1 -> new ResourceTypeIndexDocument(resourceType1, language, recursionLevels - 1))
                        .collect(Collectors.toList());
            }
        }
    }

    public static class ResourceTypeCommand implements UpdatableDto<ResourceType> {
        @JsonProperty
        @Schema(description = "If specified, the new resource type will be a child of the mentioned resource type.")
        public URI parentId;

        @JsonProperty
        @Schema(description = "If specified, set the id to this value. Must start with urn:resourcetype: and be a valid URI. If omitted, an id will be assigned automatically.", example = "urn:resourcetype:1")
        public URI id;

        @JsonProperty
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "The name of the resource type", example = "Lecture")
        public String name;

        @Override
        public Optional<URI> getId() {
            return Optional.ofNullable(id);
        }

        @Override
        public void apply(ResourceType entity) {
            entity.setName(name);
        }
    }
}
