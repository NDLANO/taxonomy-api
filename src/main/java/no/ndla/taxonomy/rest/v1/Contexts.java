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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.Translation;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.rest.v1.dtos.ContextDTO;
import no.ndla.taxonomy.rest.v1.dtos.ContextPOST;
import no.ndla.taxonomy.service.ContextUpdaterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = {"/v1/contexts"})
@Transactional(readOnly = true)
public class Contexts {
    private final NodeRepository nodeRepository;
    private final ContextUpdaterService cachedUrlUpdaterService;

    public Contexts(NodeRepository nodeRepository, ContextUpdaterService cachedUrlUpdaterService) {
        this.nodeRepository = nodeRepository;
        this.cachedUrlUpdaterService = cachedUrlUpdaterService;
    }

    @GetMapping
    @Operation(summary = "Gets all contexts")
    public List<ContextDTO> getAllContexts(
            @Parameter(description = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", required = false, defaultValue = "")
                    String language) {

        final var nodes = nodeRepository.findAllByContextIncludingCachedUrlsAndTranslations(true);

        final var contextDocuments = new ArrayList<>(nodes.stream()
                .map(topic -> new ContextDTO(
                        topic.getPublicId(),
                        topic.getTranslation(language).map(Translation::getName).orElse(topic.getName()),
                        topic.getPrimaryPath().orElse(null)))
                .toList());

        contextDocuments.sort(Comparator.comparing(ContextDTO::getId));

        return contextDocuments;
    }

    @PostMapping
    @Operation(
            summary = "Adds a new context",
            description =
                    "All subjects are already contexts and may not be added again. Only topics may be added as a context. The topic must exist already.",
            security = {@SecurityRequirement(name = "oauth")})
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public ResponseEntity<Void> createContext(
            @Parameter(name = "context", description = "the new context") @RequestBody ContextPOST command) {
        Node topic = nodeRepository.getByPublicId(command.id);
        topic.setContext(true);
        URI location = URI.create("/v1/contexts/" + topic.getPublicId());

        cachedUrlUpdaterService.updateContexts(topic);

        return ResponseEntity.created(location).build();
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Removes a context",
            description = "Does not remove the underlying resource, only marks it as not being a context",
            security = {@SecurityRequirement(name = "oauth")})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public void deleteContext(@PathVariable("id") URI id) {
        Node topic = nodeRepository.getByPublicId(id);
        topic.setContext(false);

        cachedUrlUpdaterService.updateContexts(topic);
    }
}
