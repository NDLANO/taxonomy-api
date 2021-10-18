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
import no.ndla.taxonomy.domain.Relevance;
import no.ndla.taxonomy.domain.Subject;
import no.ndla.taxonomy.domain.SubjectTopic;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.repositories.RelevanceRepository;
import no.ndla.taxonomy.repositories.SubjectRepository;
import no.ndla.taxonomy.repositories.SubjectTopicRepository;
import no.ndla.taxonomy.repositories.TopicRepository;
import no.ndla.taxonomy.service.EntityConnectionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = { "/v1/subject-topics" })
@Transactional
public class SubjectTopics {
    private final TopicRepository topicRepository;
    private final SubjectTopicRepository subjectTopicRepository;
    private final SubjectRepository subjectRepository;
    private final EntityConnectionService connectionService;
    private final RelevanceRepository relevanceRepository;

    public SubjectTopics(SubjectRepository subjectRepository, TopicRepository topicRepository,
            SubjectTopicRepository subjectTopicRepository, EntityConnectionService connectionService,
            RelevanceRepository relevanceRepository) {
        this.subjectRepository = subjectRepository;
        this.subjectTopicRepository = subjectTopicRepository;
        this.topicRepository = topicRepository;
        this.connectionService = connectionService;
        this.relevanceRepository = relevanceRepository;
    }

    @GetMapping
    @ApiOperation("Gets all connections between subjects and topics")
    public List<SubjectTopicIndexDocument> index() {
        return subjectTopicRepository.findAllIncludingSubjectAndTopic().stream().map(SubjectTopicIndexDocument::new)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @ApiOperation("Get a specific connection between a subject and a topic")
    public SubjectTopicIndexDocument get(@PathVariable("id") URI id) {
        SubjectTopic subjectTopic = subjectTopicRepository.getByPublicId(id);
        return new SubjectTopicIndexDocument(subjectTopic);
    }

    @PostMapping
    @ApiOperation("Adds a new topic to a subject")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public ResponseEntity<Void> post(
            @ApiParam(name = "command", value = "The subject and topic getting connected.") @RequestBody AddTopicToSubjectCommand command) {

        Subject subject = subjectRepository.getByPublicId(command.subjectid);
        Topic topic = topicRepository.getByPublicId(command.topicid);
        Relevance relevance = command.relevanceId != null ? relevanceRepository.getByPublicId(command.relevanceId)
                : null;

        final SubjectTopic subjectTopic;
        subjectTopic = connectionService.connectSubjectTopic(subject, topic, relevance,
                command.rank == 0 ? null : command.rank);

        URI location = URI.create("/subject-topics/" + subjectTopic.getPublicId());
        return ResponseEntity.created(location).build();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiOperation("Removes a topic from a subject")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void delete(@PathVariable("id") URI id) {
        connectionService.disconnectSubjectTopic(subjectTopicRepository.getByPublicId(id));
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiOperation(value = "Updates a connection between subject and topic", notes = "Use to update which subject is primary to a topic or to change sorting order.")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void put(@PathVariable("id") URI id,
            @ApiParam(name = "connection", value = "updated subject/topic connection") @RequestBody UpdateSubjectTopicCommand command) {
        SubjectTopic subjectTopic = subjectTopicRepository.getByPublicId(id);
        Relevance relevance = command.relevanceId != null ? relevanceRepository.getByPublicId(command.relevanceId)
                : null;

        connectionService.updateSubjectTopic(subjectTopic, relevance, command.rank > 0 ? command.rank : null);
    }

    public static class AddTopicToSubjectCommand {
        @JsonProperty
        @ApiModelProperty(required = true, value = "Subject id", example = "urn:subject:123")
        public URI subjectid;

        @JsonProperty
        @ApiModelProperty(required = true, value = "Topic id", example = "urn:topic:234")
        public URI topicid;

        @JsonProperty
        @ApiModelProperty(value = "Backwards compatibility: Always true, ignored on insert/update.", example = "true")
        public boolean primary;

        @JsonProperty
        @ApiModelProperty(value = "Order in which the topic should be sorted for the topic", example = "1")
        public int rank;

        @JsonProperty
        @ApiModelProperty(value = "Relevance id", example = "urn:relevance:core")
        public URI relevanceId;
    }

    public static class UpdateSubjectTopicCommand {
        @JsonProperty
        @ApiModelProperty(required = true, value = "connection id", example = "urn:subject-topic:2")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "If true, set this subject as the primary subject for this topic", example = "true", notes = "This will replace any other primary subject for this topic. You must have one primary subject, so it is not allowed to set the currently primary subject to not be primary any more.")
        public boolean primary;

        @JsonProperty
        @ApiModelProperty(value = "Order in which the topic should be sorted for the subject", example = "1")
        public int rank;

        @JsonProperty
        @ApiModelProperty(value = "Relevance id", example = "urn:relevance:core")
        public URI relevanceId;
    }

    public static class SubjectTopicIndexDocument {
        @JsonProperty
        @ApiModelProperty(value = "Subject id", example = "urn:subject:123")
        public URI subjectid;

        @JsonProperty
        @ApiModelProperty(value = "Topic id", example = "urn:topic:345")
        public URI topicid;

        @JsonProperty
        @ApiModelProperty(value = "Connection id", example = "urn:subject-has-topics:34")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "primary", example = "true")
        public boolean primary;

        @JsonProperty
        @ApiModelProperty(value = "Order in which the topic is sorted under the subject", example = "1")
        public int rank;

        @JsonProperty
        @ApiModelProperty(value = "Relevance id", example = "urn:relevance:core")
        public URI relevanceId;

        SubjectTopicIndexDocument() {
        }

        SubjectTopicIndexDocument(SubjectTopic subjectTopic) {
            id = subjectTopic.getPublicId();

            subjectid = subjectTopic.getSubject().map(Subject::getPublicId).orElse(null);

            topicid = subjectTopic.getTopic().map(Topic::getPublicId).orElse(null);

            primary = true;
            rank = subjectTopic.getRank();
            relevanceId = subjectTopic.getRelevance().map(Relevance::getPublicId).orElse(null);
        }
    }
}
