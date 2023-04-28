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
import no.ndla.taxonomy.domain.Relevance;
import no.ndla.taxonomy.domain.Translation;
import no.ndla.taxonomy.domain.exceptions.NotFoundException;
import no.ndla.taxonomy.repositories.RelevanceRepository;
import no.ndla.taxonomy.rest.v1.NodeTranslations.TranslationDTO;
import no.ndla.taxonomy.service.UpdatableDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = { "/v1/relevances" })
public class Relevances extends CrudController<Relevance> {

    private final RelevanceRepository relevanceRepository;

    public Relevances(RelevanceRepository repository) {
        super(repository);
        relevanceRepository = repository;
    }

    @GetMapping
    @Operation(summary = "Gets all relevances")
    @Transactional(readOnly = true)
    public List<RelevanceDTO> index(
            @Parameter(description = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", required = false, defaultValue = "") String language) {
        return relevanceRepository.findAllIncludingTranslations().stream()
                .map(relevance -> new RelevanceDTO(relevance, language)).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Gets a single relevance", description = "Default language will be returned if desired language not found or if parameter is omitted.")
    @Transactional(readOnly = true)
    public RelevanceDTO get(@PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", required = false, defaultValue = "") String language) {
        return relevanceRepository.findFirstByPublicIdIncludingTranslations(id)
                .map(relevance -> new RelevanceDTO(relevance, language))
                .orElseThrow(() -> new NotFoundException("Relevance", id));
    }

    @PostMapping
    @Operation(summary = "Creates a new relevance", security = { @SecurityRequirement(name = "oauth") })
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public ResponseEntity<Void> post(
            @Parameter(name = "relevance", description = "The new relevance") @RequestBody RelevancePUT command) {
        return doPost(new Relevance(), command);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Updates a relevance", security = { @SecurityRequirement(name = "oauth") })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public void put(@PathVariable("id") URI id,
            @Parameter(name = "relevance", description = "The updated relevance. Fields not included will be set to null.") @RequestBody RelevancePUT command) {
        doPut(id, command);
    }

    @Schema(name = "Relevance")
    public static class RelevanceDTO {
        @JsonProperty
        @Schema(example = "urn:relevance:core")
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

    public static class RelevancePUT implements UpdatableDto<Relevance> {
        @JsonProperty
        @Schema(description = "If specified, set the id to this value. Must start with urn:relevance: and be a valid URI. If ommitted, an id will be assigned automatically. Ignored on update", example = "urn:relevance:supplementary")
        public URI id;

        @JsonProperty
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "The name of the relevance", example = "Supplementary")
        public String name;

        @Override
        public Optional<URI> getId() {
            return Optional.ofNullable(id);
        }

        @Override
        public void apply(Relevance entity) {
            entity.setName(name);
        }
    }
}
