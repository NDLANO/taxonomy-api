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
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.domain.TopicTranslation;
import no.ndla.taxonomy.domain.exceptions.NotFoundException;
import no.ndla.taxonomy.repositories.TopicRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(path = { "/v1/topics/{id}/translations" })
@Transactional
public class TopicTranslations {

    private final TopicRepository topicRepository;

    private final EntityManager entityManager;

    public TopicTranslations(TopicRepository topicRepository, EntityManager entityManager) {
        this.topicRepository = topicRepository;
        this.entityManager = entityManager;
    }

    @GetMapping
    @ApiOperation("Gets all relevanceTranslations for a single topic")
    public List<TopicTranslations.TopicTranslationIndexDocument> index(@PathVariable("id") URI id) {
        Topic topic = topicRepository.getByPublicId(id);
        List<TopicTranslations.TopicTranslationIndexDocument> result = new ArrayList<>();
        topic.getTranslations().forEach(t -> result.add(new TopicTranslations.TopicTranslationIndexDocument() {
            {
                name = t.getName();
                language = t.getLanguageCode();
            }
        }));
        return result;
    }

    @GetMapping("/{language}")
    @ApiOperation("Gets a single translation for a single topic")
    public TopicTranslations.TopicTranslationIndexDocument get(@PathVariable("id") URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb", required = true) @PathVariable("language") String language) {
        Topic topic = topicRepository.getByPublicId(id);
        TopicTranslation translation = topic.getTranslation(language).orElseThrow(
                () -> new NotFoundException("translation with language code " + language + " for topic", id));
        return new TopicTranslations.TopicTranslationIndexDocument() {
            {
                name = translation.getName();
                language = translation.getLanguageCode();
            }
        };
    }

    @PutMapping("/{language}")
    @ApiOperation("Creates or updates a translation of a topic")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void put(@PathVariable("id") URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb", required = true) @PathVariable("language") String language,
            @ApiParam(name = "topic", value = "The new or updated translation") @RequestBody TopicTranslations.UpdateTopicTranslationCommand command) {
        Topic topic = topicRepository.getByPublicId(id);
        TopicTranslation translation = topic.addTranslation(language);
        entityManager.persist(translation);
        translation.setName(command.name);
    }

    @DeleteMapping("/{language}")
    @ApiOperation("Deletes a translation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void delete(@PathVariable("id") URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb", required = true) @PathVariable("language") String language) {
        Topic topic = topicRepository.getByPublicId(id);
        topic.getTranslation(language).ifPresent(topicTranslation -> {
            topic.removeTranslation(language);
            entityManager.remove(topicTranslation);
        });
    }

    public static class TopicTranslationIndexDocument {
        @JsonProperty
        @ApiModelProperty(value = "The translated name of the topic", example = "Trigonometry")
        public String name;

        @JsonProperty
        @ApiModelProperty(value = "ISO 639-1 language code", example = "en")
        public String language;
    }

    public static class UpdateTopicTranslationCommand {
        @JsonProperty
        @ApiModelProperty(value = "The translated name of the topic", example = "Trigonometry")
        public String name;
    }
}
