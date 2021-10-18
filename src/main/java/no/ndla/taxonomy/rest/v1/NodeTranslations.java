/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeTranslation;
import no.ndla.taxonomy.domain.exceptions.NotFoundException;
import no.ndla.taxonomy.repositories.NodeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(path = { "/v1/nodes/{id}/translations" })
@Transactional
public class NodeTranslations {

    private final NodeRepository nodeRepository;

    private final EntityManager entityManager;

    public NodeTranslations(NodeRepository nodeRepository, EntityManager entityManager) {
        this.nodeRepository = nodeRepository;
        this.entityManager = entityManager;
    }

    @GetMapping
    @ApiOperation("Gets all translations for a single node")
    public List<TranslationDTO> index(@PathVariable("id") URI id) {
        Node node = nodeRepository.getByPublicId(id);
        List<TranslationDTO> result = new ArrayList<>();
        node.getTranslations().forEach(t -> result.add(new TranslationDTO() {
            {
                name = t.getName();
                language = t.getLanguageCode();
            }
        }));
        return result;
    }

    @GetMapping("/{language}")
    @ApiOperation("Gets a single translation for a single node")
    public TranslationDTO get(@PathVariable("id") URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb", required = true) @PathVariable("language") String language) {
        Node node = nodeRepository.getByPublicId(id);
        NodeTranslation translation = node.getTranslation(language).orElseThrow(
                () -> new NotFoundException("translation with language code " + language + " for node", id));
        return new TranslationDTO() {
            {
                name = translation.getName();
                language = translation.getLanguageCode();
            }
        };
    }

    @PutMapping("/{language}")
    @ApiOperation("Creates or updates a translation of a node")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void put(@PathVariable("id") URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb", required = true) @PathVariable("language") String language,
            @ApiParam(name = "command", value = "The new or updated translation") @RequestBody UpdateTranslationCommand command) {
        Node node = nodeRepository.getByPublicId(id);
        NodeTranslation translation = node.addTranslation(language);
        entityManager.persist(translation);
        translation.setName(command.name);
    }

    @DeleteMapping("/{language}")
    @ApiOperation("Deletes a translation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void delete(@PathVariable("id") URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb", required = true) @PathVariable("language") String language) {
        Node node = nodeRepository.getByPublicId(id);
        node.getTranslation(language).ifPresent(translation -> {
            node.removeTranslation(language);
            entityManager.remove(translation);
        });
    }

    public static class TranslationDTO {
        @JsonProperty
        @ApiModelProperty(value = "The translated name of the node", example = "Trigonometry")
        public String name;

        @JsonProperty
        @ApiModelProperty(value = "ISO 639-1 language code", example = "en")
        public String language;
    }

    public static class UpdateTranslationCommand {
        @JsonProperty
        @ApiModelProperty(value = "The translated name of the node", example = "Trigonometry")
        public String name;
    }
}
