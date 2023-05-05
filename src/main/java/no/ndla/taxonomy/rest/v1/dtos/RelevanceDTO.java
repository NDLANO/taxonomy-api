/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import no.ndla.taxonomy.domain.Relevance;
import no.ndla.taxonomy.domain.Translation;
import no.ndla.taxonomy.service.dtos.TranslationDTO;

import java.net.URI;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Schema(name = "Relevance")
public class RelevanceDTO {
    @JsonProperty
    @Schema(description = "Specifies if node is core or supplementary", example = "urn:relevance:core")
    public URI id;

    @JsonProperty
    @Schema(description = "The name of the relevance", example = "Core")
    public String name;

    @JsonProperty
    @Schema(description = "All translations of this relevance")
    private Set<TranslationDTO> translations;

    @JsonProperty
    @Schema(description = "List of language codes supported by translations")
    private Set<String> supportedLanguages;

    public RelevanceDTO() {
    }

    public RelevanceDTO(Relevance relevance, String language) {
        this.id = relevance.getPublicId();

        var translations = relevance.getTranslations();
        this.translations = translations.stream().map(TranslationDTO::new).collect(Collectors.toSet());
        this.supportedLanguages = this.translations.stream().map(t -> t.language).collect(Collectors.toSet());

        this.name = translations.stream().filter(t -> Objects.equals(t.getLanguageCode(), language)).findFirst()
                .map(Translation::getName).orElse(relevance.getName());
    }
}
