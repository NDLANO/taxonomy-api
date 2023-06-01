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
@RequestMapping(path = {"/v1/subjects/{id}/translations"})
public class SubjectTranslations {
    private final NodeRepository nodeRepository;

    private final EntityManager entityManager;

    public SubjectTranslations(NodeRepository nodeRepository, EntityManager entityManager) {
        this.nodeRepository = nodeRepository;
        this.entityManager = entityManager;
    }

    @Deprecated
    @GetMapping
    @Operation(summary = "Gets all relevanceTranslations for a single subject")
    @Transactional(readOnly = true)
    public List<TranslationDTO> getAllSubjectTranslations(@PathVariable("id") URI id) {
        Node subject = nodeRepository.getByPublicId(id);
        List<TranslationDTO> result = new ArrayList<>();
        subject.getTranslations()
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
    @Operation(summary = "Gets a single translation for a single subject")
    @Transactional(readOnly = true)
    public TranslationDTO getSubjectTranslation(
            @PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb", required = true)
                    @PathVariable("language")
                    String language) {
        Node subject = nodeRepository.getByPublicId(id);
        var translation = subject.getTranslation(language)
                .orElseThrow(
                        () -> new NotFoundException("translation with language code " + language + " for subject", id));

        return new TranslationDTO() {
            {
                name = translation.getName();
                language = translation.getLanguageCode();
            }
        };
    }

    @Deprecated
    @DeleteMapping("/{language}")
    @Operation(
            summary = "Deletes a translation",
            security = {@SecurityRequirement(name = "oauth")})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public void deleteSubjectTranslation(
            @PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb", required = true)
                    @PathVariable("language")
                    String language) {
        Node subject = nodeRepository.getByPublicId(id);
        subject.getTranslation(language).ifPresent((translation) -> {
            subject.removeTranslation(language);
            entityManager.persist(subject);
        });
    }

    @Deprecated
    @PutMapping("/{language}")
    @Operation(
            summary = "Creates or updates a translation of a subject",
            security = {@SecurityRequirement(name = "oauth")})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public void createUpdateSubjectTranslation(
            @PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb", required = true)
                    @PathVariable("language")
                    String language,
            @Parameter(name = "subject", description = "The new or updated translation") @RequestBody
                    TranslationPUT command) {
        Node subject = nodeRepository.getByPublicId(id);
        subject.addTranslation(command.name, language);
        entityManager.persist(subject);
    }
}
