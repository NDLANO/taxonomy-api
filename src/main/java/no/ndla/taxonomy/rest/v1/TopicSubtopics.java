package no.ndla.taxonomy.rest.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.PrimaryParentRequiredException;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.domain.TopicSubtopic;
import no.ndla.taxonomy.repositories.TopicRepository;
import no.ndla.taxonomy.repositories.TopicSubtopicRepository;
import no.ndla.taxonomy.rest.BadHttpRequestException;
import no.ndla.taxonomy.rest.ConflictHttpResponseException;
import no.ndla.taxonomy.rest.NotFoundHttpRequestException;
import no.ndla.taxonomy.service.EntityConnectionService;
import no.ndla.taxonomy.service.exceptions.DuplicateConnectionException;
import no.ndla.taxonomy.service.exceptions.InvalidArgumentServiceException;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = {"/v1/topic-subtopics"})
@Transactional
public class TopicSubtopics {
    private final TopicRepository topicRepository;
    private final TopicSubtopicRepository topicSubtopicRepository;
    private final EntityConnectionService connectionService;

    public TopicSubtopics(TopicRepository topicRepository, TopicSubtopicRepository topicSubtopicRepository,
                          EntityConnectionService connectionService) {
        this.topicRepository = topicRepository;
        this.topicSubtopicRepository = topicSubtopicRepository;
        this.connectionService = connectionService;
    }

    @GetMapping
    @ApiOperation(value = "Gets all connections between topics and subtopics")
    public List<TopicSubtopicIndexDocument> index() {
        return topicSubtopicRepository
                .findAllIncludingTopicAndSubtopic()
                .stream()
                .map(TopicSubtopicIndexDocument::new)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "Gets a single connection between a topic and a subtopic")
    public TopicSubtopicIndexDocument get(@PathVariable("id") URI id) {
        TopicSubtopic topicSubtopic = topicSubtopicRepository.getByPublicId(id);
        return new TopicSubtopicIndexDocument(topicSubtopic);
    }

    @PostMapping
    @ApiOperation(value = "Adds a subtopic to a topic")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public ResponseEntity<Void> post(
            @ApiParam(name = "connection", value = "The new connection") @RequestBody AddSubtopicToTopicCommand command) {

        Topic topic = topicRepository.getByPublicId(command.topicid);
        Topic subtopic = topicRepository.getByPublicId(command.subtopicid);

        final TopicSubtopic topicSubtopic;
        try {
            topicSubtopic = connectionService.connectTopicSubtopic(topic, subtopic, command.primary, command.rank == 0 ? null : command.rank);
        } catch (DuplicateConnectionException e) {
            throw new ConflictHttpResponseException(e);
        } catch (InvalidArgumentServiceException e) {
            throw new BadHttpRequestException(e);
        }

        URI location = URI.create("/topic-subtopics/" + topicSubtopic.getPublicId());
        return ResponseEntity.created(location).build();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiOperation(value = "Removes a connection between a topic and a subtopic")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void delete(@PathVariable("id") URI id) {
        connectionService.disconnectTopicSubtopic(topicSubtopicRepository.getByPublicId(id));
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiOperation(value = "Updates a connection between a topic and a subtopic", notes = "Use to update which topic is primary to a subtopic or to alter sorting order")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void put(@PathVariable("id") URI id,
                    @ApiParam(name = "connection", value = "The updated connection") @RequestBody UpdateTopicSubtopicCommand command) {
        final var topicSubtopic = topicSubtopicRepository.getByPublicId(id);

        if (topicSubtopic.isPrimary() && !command.primary) {
            throw new PrimaryParentRequiredException();
        }

        try {
            connectionService.updateTopicSubtopic(topicSubtopic, command.primary, command.rank > 0 ? command.rank : null);
        } catch (InvalidArgumentServiceException e) {
            throw new BadHttpRequestException(e);
        } catch (NotFoundServiceException e) {
            throw new NotFoundHttpRequestException(e);
        }
    }

    public static class AddSubtopicToTopicCommand {
        @JsonProperty
        @ApiModelProperty(required = true, value = "Topic id", example = "urn:topic:234")
        public URI topicid;

        @JsonProperty
        @ApiModelProperty(required = true, value = "Subtopic id", example = "urn:topic:234")
        public URI subtopicid;

        @JsonProperty
        @ApiModelProperty(value = "Primary connection", example = "true")
        public boolean primary = true;

        @JsonProperty
        @ApiModelProperty(value = "Order in which to sort the subtopic for the topic", example = "1")
        public int rank;
    }

    public static class UpdateTopicSubtopicCommand {
        @JsonProperty
        @ApiModelProperty(value = "Connection id", example = "urn:topic-has-subtopics:345")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "Primary connection", example = "true")
        public boolean primary;

        @JsonProperty
        @ApiModelProperty(value = "Order in which subtopic is sorted for the topic", example = "1")
        public int rank;
    }

    public static class TopicSubtopicIndexDocument {
        @JsonProperty
        @ApiModelProperty(value = "Topic id", example = "urn:topic:234")
        public URI topicid;

        @JsonProperty
        @ApiModelProperty(value = "Subtopic id", example = "urn:topic:234")
        public URI subtopicid;

        @JsonProperty
        @ApiModelProperty(value = "Connection id", example = "urn:topic-has-subtopics:345")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "Primary connection", example = "true")
        public boolean primary;

        @JsonProperty
        @ApiModelProperty(value = "Order in which subtopic is sorted for the topic", example = "1")
        public int rank;

        TopicSubtopicIndexDocument() {
        }

        TopicSubtopicIndexDocument(TopicSubtopic topicSubtopic) {
            id = topicSubtopic.getPublicId();
            topicSubtopic.getTopic().ifPresent(topic -> topicid = topic.getPublicId());
            topicSubtopic.getSubtopic().ifPresent(subtopic -> subtopicid = subtopic.getPublicId());
            primary = topicSubtopic.isPrimary();
            rank = topicSubtopic.getRank();
        }
    }
}
