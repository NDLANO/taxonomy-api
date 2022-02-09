/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.ndla.taxonomy.domain.ResourceType;
import no.ndla.taxonomy.domain.Translation;
import no.ndla.taxonomy.rest.v1.NodeTranslations.TranslationDTO;

import java.net.URI;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@ApiModel("ResourceType")
public class ResourceTypeDTO implements Comparable<ResourceTypeDTO> {
    @JsonProperty
    @ApiModelProperty(example = "urn:resourcetype:2")
    private URI id;

    @JsonProperty
    @ApiModelProperty(example = "urn:resourcetype:1")
    private URI parentId;

    @JsonProperty
    @ApiModelProperty(value = "The name of the resource type", example = "Lecture")
    private String name;

    @JsonProperty
    @ApiModelProperty(value = "All translations of this resource type")
    private Set<TranslationDTO> translations;

    @JsonProperty
    @ApiModelProperty(value = "List of language codes supported by translations")
    private Set<String> supportedLanguages;

    public ResourceTypeDTO() {
    }

    public ResourceTypeDTO(ResourceType resourceType, String languageCode) {
        this.id = resourceType.getPublicId();

        var translations = resourceType.getTranslations();
        this.translations = translations.stream().map(TranslationDTO::new).collect(Collectors.toSet());
        this.supportedLanguages = this.translations.stream().map(t -> t.language).collect(Collectors.toSet());

        resourceType.getParent().map(ResourceType::getPublicId).ifPresent(publicId -> this.parentId = publicId);

        this.name = translations.stream().filter(t -> Objects.equals(t.getLanguageCode(), languageCode)).findFirst()
                .map(Translation::getName).orElse(resourceType.getName());
    }

    public URI getId() {
        return id;
    }

    public URI getParentId() {
        return parentId;
    }

    public String getName() {
        return name;
    }

    public Set<TranslationDTO> getTranslations() {
        return translations;
    }

    @Override
    public int compareTo(ResourceTypeDTO o) {
        // We want to sort resourceTypes without parents first when sorting
        if (this.parentId == null && o.parentId != null)
            return 1;
        return -1;
    }
}
