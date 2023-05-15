/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import no.ndla.taxonomy.domain.JsonTranslation;
import no.ndla.taxonomy.domain.ResourceType;
import no.ndla.taxonomy.rest.v1.ResourceTypes;
import no.ndla.taxonomy.service.dtos.TranslationDTO;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Schema(name = "ResourceType")
public class ResourceTypeDTO {
    @JsonProperty
    @Schema(example = "urn:resourcetype:1")
    public URI id;

    @JsonProperty
    @Schema(description = "The name of the resource type", example = "Lecture")
    public String name;

    @JsonProperty
    @Schema(description = "Sub resource types")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public List<ResourceTypeDTO> subtypes = new ArrayList<>();

    @JsonProperty
    @Schema(description = "All translations of this resource type")
    private Set<TranslationDTO> translations;

    @JsonProperty
    @Schema(description = "List of language codes supported by translations")
    private Set<String> supportedLanguages;

    public ResourceTypeDTO() {
    }

    public ResourceTypeDTO(ResourceType resourceType, String language, int recursionLevels) {
        this.id = resourceType.getPublicId();

        var translations = resourceType.getTranslations();
        this.translations = translations.stream().map(TranslationDTO::new).collect(Collectors.toSet());
        this.supportedLanguages = this.translations.stream().map(t -> t.language).collect(Collectors.toSet());

        this.name = resourceType.getTranslation(language).map(JsonTranslation::getName).orElse(resourceType.getName());

        if (recursionLevels > 0) {
            this.subtypes = resourceType.getSubtypes().stream()
                    .map(resourceType1 -> new ResourceTypeDTO(resourceType1, language, recursionLevels - 1))
                    .collect(Collectors.toList());
        }
    }
}
