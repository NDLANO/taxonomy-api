/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.SubjectTranslation;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.domain.TopicTranslation;
import no.ndla.taxonomy.repositories.SubjectRepository;
import no.ndla.taxonomy.repositories.TopicRepository;
import no.ndla.taxonomy.service.CachedUrlUpdaterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = { "/v1/contexts" })
@Transactional(readOnly = true)
public class Contexts {
    private final TopicRepository topicRepository;
    private final SubjectRepository subjectRepository;
    private final CachedUrlUpdaterService cachedUrlUpdaterService;

    public Contexts(TopicRepository topicRepository, SubjectRepository subjectRepository,
            CachedUrlUpdaterService cachedUrlUpdaterService) {
        this.topicRepository = topicRepository;
        this.subjectRepository = subjectRepository;
        this.cachedUrlUpdaterService = cachedUrlUpdaterService;
    }

    @GetMapping
    public List<ContextIndexDocument> get(
            @ApiParam(value = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", required = false, defaultValue = "") String language) {

        final var subjects = subjectRepository.findAllIncludingCachedUrlsAndTranslations();
        final var topics = topicRepository.findAllByContextIncludingCachedUrlsAndTranslations(true);

        final var contextDocuments = new ArrayList<ContextIndexDocument>();

        contextDocuments
                .addAll(subjects.stream()
                        .map(subject -> new ContextIndexDocument(subject.getPublicId(),
                                subject.getTranslation(language).map(SubjectTranslation::getName)
                                        .orElse(subject.getName()),
                                subject.getPrimaryPath().orElse(null)))
                        .collect(Collectors.toList()));

        contextDocuments.addAll(topics.stream()
                .map(topic -> new ContextIndexDocument(topic.getPublicId(),
                        topic.getTranslation(language).map(TopicTranslation::getName).orElse(topic.getName()),
                        topic.getPrimaryPath().orElse(null)))
                .collect(Collectors.toList()));

        contextDocuments.sort(Comparator.comparing(ContextIndexDocument::getId));

        return contextDocuments;
    }

    @PostMapping
    @ApiOperation(value = "Adds a new context", notes = "All subjects are already contexts and may not be added again. Only topics may be added as a context. The topic must exist already.")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public ResponseEntity<Void> post(
            @ApiParam(name = "context", value = "the new context") @RequestBody CreateContextCommand command) {
        Topic topic = topicRepository.getByPublicId(command.id);
        topic.setContext(true);
        URI location = URI.create("/v1/contexts/" + topic.getPublicId());

        cachedUrlUpdaterService.updateCachedUrls(topic);

        return ResponseEntity.created(location).build();
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "Removes a context", notes = "Does not remove the underlying resource, only marks it as not being a context")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public void delete(@PathVariable("id") URI id) {
        Topic topic = topicRepository.getByPublicId(id);
        topic.setContext(false);

        cachedUrlUpdaterService.updateCachedUrls(topic);
    }

    public static class ContextIndexDocument {
        public URI id;
        public String path;
        public String name;

        private ContextIndexDocument(URI id, String name, String path) {
            this.id = id;
            this.name = name;
            this.path = path;
        }

        public URI getId() {
            return id;
        }

        public String getPath() {
            return path;
        }

        public String getName() {
            return name;
        }
    }

    public static class CreateContextCommand {
        public URI id;
    }
}
