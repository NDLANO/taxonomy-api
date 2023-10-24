/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.persistence.EntityManager;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import no.ndla.taxonomy.domain.Relevance;
import no.ndla.taxonomy.domain.exceptions.NotFoundException;
import no.ndla.taxonomy.repositories.RelevanceRepository;
import no.ndla.taxonomy.rest.v1.dtos.TranslationPUT;
import no.ndla.taxonomy.service.dtos.TranslationDTO;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = {"/v1/relevances/{id}/translations", "/v1/relevances/{id}/translations/"})
public class RelevanceTranslations {

    private final RelevanceRepository relevanceRepository;

    private final EntityManager entityManager;

    public RelevanceTranslations(RelevanceRepository relevanceRepository, EntityManager entityManager) {
        this.relevanceRepository = relevanceRepository;
        this.entityManager = entityManager;
    }

    @GetMapping
    @Operation(summary = "Gets all relevanceTranslations for a single relevance")
    @Transactional(readOnly = true)
    public List<TranslationDTO> getAllRelevanceTranslations(@PathVariable("id") URI id) {
        Relevance relevance = relevanceRepository.getByPublicId(id);
        List<TranslationDTO> result = new ArrayList<>();
        relevance
                .getTranslations()
                .forEach(t -> result.add(new TranslationDTO() {
                    {
                        name = t.getName();
                        language = t.getLanguageCode();
                    }
                }));
        return result;
    }

    @GetMapping("/{language}")
    @Operation(summary = "Gets a single translation for a single relevance")
    @Transactional(readOnly = true)
    public TranslationDTO getRelevanceTranslation(
            @PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb", required = true)
                    @PathVariable("language")
                    String language) {
        Relevance relevance = relevanceRepository.getByPublicId(id);
        var translation = relevance
                .getTranslation(language)
                .orElseThrow(() ->
                        new NotFoundException("translation with language code " + language + " for relevance", id));

        return new TranslationDTO() {
            {
                name = translation.getName();
                language = translation.getLanguageCode();
            }
        };
    }

    @PutMapping("/{language}")
    @Operation(
            summary = "Creates or updates a translation of a relevance",
            security = {@SecurityRequirement(name = "oauth")})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_ADMIN')")
    @Transactional
    public void createUpdateRelevanceTranslation(
            @PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb", required = true)
                    @PathVariable("language")
                    String language,
            @Parameter(name = "resourceType", description = "The new or updated translation") @RequestBody
                    TranslationPUT command) {
        Relevance relevance = relevanceRepository.getByPublicId(id);
        relevance.addTranslation(command.name, language);
        entityManager.persist(relevance);
    }

    @DeleteMapping("/{language}")
    @Operation(
            summary = "Deletes a translation",
            security = {@SecurityRequirement(name = "oauth")})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_ADMIN')")
    @Transactional
    public void deleteRelevanceTranslation(
            @PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb", required = true)
                    @PathVariable("language")
                    String language) {
        Relevance relevance = relevanceRepository.getByPublicId(id);
        relevance.getTranslation(language).ifPresent((translation) -> {
            relevance.removeTranslation(language);
            entityManager.persist(relevance);
        });
    }
}
