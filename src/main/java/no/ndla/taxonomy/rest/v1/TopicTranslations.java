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
import java.util.List;
import javax.persistence.EntityManager;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.exceptions.NotFoundException;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.rest.v1.dtos.TranslationPUT;
import no.ndla.taxonomy.service.dtos.TranslationDTO;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = {"/v1/topics/{id}/translations"})
public class TopicTranslations {

    private final NodeRepository nodeRepository;

    private final EntityManager entityManager;

    public TopicTranslations(NodeRepository nodeRepository, EntityManager entityManager) {
        this.nodeRepository = nodeRepository;
        this.entityManager = entityManager;
    }

    @Deprecated
    @GetMapping
    @Operation(summary = "Gets all relevanceTranslations for a single topic")
    @Transactional(readOnly = true)
    public List<TranslationDTO> getAllTopicTranslations(@PathVariable("id") URI id) {
        Node topic = nodeRepository.getByPublicId(id);
        List<TranslationDTO> result = new ArrayList<>();
        topic.getTranslations()
                .forEach(t -> result.add(new TranslationDTO() {
                    {
                        name = t.getName();
                        language = t.getLanguageCode();
                    }
                }));
        return result;
    }

    @Deprecated
    @GetMapping("/{language}")
    @Operation(summary = "Gets a single translation for a single topic")
    @Transactional(readOnly = true)
    public TranslationDTO getTopicTranslation(
            @PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb", required = true)
                    @PathVariable("language")
                    String language) {
        Node topic = nodeRepository.getByPublicId(id);
        var translation = topic.getTranslation(language)
                .orElseThrow(
                        () -> new NotFoundException("translation with language code " + language + " for topic", id));
        return new TranslationDTO() {
            {
                name = translation.getName();
                language = translation.getLanguageCode();
            }
        };
    }

    @Deprecated
    @PutMapping("/{language}")
    @Operation(
            summary = "Creates or updates a translation of a topic",
            security = {@SecurityRequirement(name = "oauth")})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public void createUpdateTopicTranslation(
            @PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb", required = true)
                    @PathVariable("language")
                    String language,
            @Parameter(name = "topic", description = "The new or updated translation") @RequestBody
                    TranslationPUT command) {
        var topic = nodeRepository.getByPublicId(id);
        topic.addTranslation(command.name, language);
        entityManager.persist(topic);
    }

    @Deprecated
    @DeleteMapping("/{language}")
    @Operation(
            summary = "Deletes a translation",
            security = {@SecurityRequirement(name = "oauth")})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public void deleteTopicTranslation(
            @PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb", required = true)
                    @PathVariable("language")
                    String language) {
        Node topic = nodeRepository.getByPublicId(id);
        topic.getTranslation(language).ifPresent(topicTranslation -> {
            topic.removeTranslation(language);
            entityManager.persist(topic);
        });
    }
}
