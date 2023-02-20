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
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.exceptions.NotFoundException;
import no.ndla.taxonomy.repositories.NodeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityManager;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(path = { "/v1/subjects/{id}/translations" })
@Transactional(readOnly = true)
public class SubjectTranslations {
    private final NodeRepository nodeRepository;

    private final EntityManager entityManager;

    public SubjectTranslations(NodeRepository nodeRepository, EntityManager entityManager) {
        this.nodeRepository = nodeRepository;
        this.entityManager = entityManager;
    }

    @GetMapping
    @Operation(summary = "Gets all relevanceTranslations for a single subject")
    public List<SubjectTranslationIndexDocument> index(@PathVariable("id") URI id) {
        Node subject = nodeRepository.getByPublicId(id);
        List<SubjectTranslationIndexDocument> result = new ArrayList<>();
        subject.getTranslations().forEach(t -> result.add(new SubjectTranslationIndexDocument() {
            {
                name = t.getName();
                language = t.getLanguageCode();
            }
        }));
        return result;
    }

    @GetMapping("/{language}")
    @Operation(summary = "Gets a single translation for a single subject")
    public SubjectTranslationIndexDocument get(@PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb", required = true) @PathVariable("language") String language) {
        Node subject = nodeRepository.getByPublicId(id);
        var translation = subject.getTranslation(language).orElseThrow(
                () -> new NotFoundException("translation with language code " + language + " for subject", id));

        return new SubjectTranslationIndexDocument() {
            {
                name = translation.getName();
                language = translation.getLanguageCode();
            }
        };
    }

    @DeleteMapping("/{language}")
    @Operation(summary = "Deletes a translation", security = { @SecurityRequirement(name = "oauth") })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void delete(@PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb", required = true) @PathVariable("language") String language) {
        Node subject = nodeRepository.getByPublicId(id);
        subject.getTranslation(language).ifPresent((translation) -> {
            subject.removeTranslation(language);
            entityManager.persist(subject);
        });
    }

    @PutMapping("/{language}")
    @Operation(summary = "Creates or updates a translation of a subject", security = {
            @SecurityRequirement(name = "oauth") })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void put(@PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb", required = true) @PathVariable("language") String language,
            @Parameter(name = "subject", description = "The new or updated translation") @RequestBody UpdateSubjectTranslationCommand command) {
        Node subject = nodeRepository.getByPublicId(id);
        subject.addTranslation(command.name, language);
        entityManager.persist(subject);
    }

    public static class SubjectTranslationIndexDocument {
        @JsonProperty
        @Schema(description = "The translated name of the subject", example = "Mathematics")
        public String name;

        @JsonProperty
        @Schema(description = "ISO 639-1 language code", example = "en")
        public String language;
    }

    public static class UpdateSubjectTranslationCommand {
        @JsonProperty
        @Schema(description = "The translated name of the subject", example = "Mathematics")
        public String name;
    }
}
