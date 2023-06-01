/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import no.ndla.taxonomy.domain.Relevance;
import no.ndla.taxonomy.domain.exceptions.NotFoundException;
import no.ndla.taxonomy.repositories.RelevanceRepository;
import no.ndla.taxonomy.rest.v1.dtos.RelevanceDTO;
import no.ndla.taxonomy.rest.v1.dtos.RelevancePUT;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = {"/v1/relevances"})
public class Relevances extends CrudController<Relevance> {

    private final RelevanceRepository relevanceRepository;

    public Relevances(RelevanceRepository repository) {
        super(repository);
        relevanceRepository = repository;
    }

    @GetMapping
    @Operation(summary = "Gets all relevances")
    @Transactional(readOnly = true)
    public List<RelevanceDTO> getAllRelevances(
            @Parameter(description = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", required = false, defaultValue = "")
                    String language) {
        return relevanceRepository.findAllIncludingTranslations().stream()
                .map(relevance -> new RelevanceDTO(relevance, language))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Gets a single relevance",
            description = "Default language will be returned if desired language not found or if parameter is omitted.")
    @Transactional(readOnly = true)
    public RelevanceDTO getRelevance(
            @PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", required = false, defaultValue = "")
                    String language) {
        return relevanceRepository
                .findFirstByPublicIdIncludingTranslations(id)
                .map(relevance -> new RelevanceDTO(relevance, language))
                .orElseThrow(() -> new NotFoundException("Relevance", id));
    }

    @PostMapping
    @Operation(
            summary = "Creates a new relevance",
            security = {@SecurityRequirement(name = "oauth")})
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public ResponseEntity<Void> createRelevance(
            @Parameter(name = "relevance", description = "The new relevance") @RequestBody RelevancePUT command) {
        return createEntity(new Relevance(), command);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Updates a relevance",
            security = {@SecurityRequirement(name = "oauth")})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public void updateRelevance(
            @PathVariable("id") URI id,
            @Parameter(
                            name = "relevance",
                            description = "The updated relevance. Fields not included will be set to null.")
                    @RequestBody
                    RelevancePUT command) {
        updateEntity(id, command);
    }
}
