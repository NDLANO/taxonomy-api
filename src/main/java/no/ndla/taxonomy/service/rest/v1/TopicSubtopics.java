package no.ndla.taxonomy.service.rest.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.service.domain.PrimaryParentRequiredException;
import no.ndla.taxonomy.service.domain.Topic;
import no.ndla.taxonomy.service.domain.TopicSubtopic;
import no.ndla.taxonomy.service.repositories.TopicRepository;
import no.ndla.taxonomy.service.repositories.TopicSubtopicRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(path = {"/v1/topic-subtopics"})
@Transactional
public class TopicSubtopics {
    private TopicRepository topicRepository;
    private TopicSubtopicRepository topicSubtopicRepository;

    public TopicSubtopics(TopicRepository topicRepository, TopicSubtopicRepository topicSubtopicRepository) {
        this.topicRepository = topicRepository;
        this.topicSubtopicRepository = topicSubtopicRepository;
    }

    @GetMapping
    @ApiOperation(value = "Gets all connections between topics and subtopics")
    @PreAuthorize("hasAuthority('READONLY')")
    public List<TopicSubtopicIndexDocument> index() throws Exception {
        List<TopicSubtopicIndexDocument> result = new ArrayList<>();

        topicSubtopicRepository.findAll().forEach(record -> result.add(new TopicSubtopicIndexDocument(record)));
        return result;
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "Gets a single connection between a topic and a subtopic")
    @PreAuthorize("hasAuthority('READONLY')")
    public TopicSubtopicIndexDocument get(@PathVariable("id") URI id) throws Exception {
        TopicSubtopic topicSubtopic = topicSubtopicRepository.getByPublicId(id);
        TopicSubtopicIndexDocument result = new TopicSubtopicIndexDocument(topicSubtopic);
        return result;
    }

    @PostMapping
    @ApiOperation(value = "Adds a subtopic to a topic")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public ResponseEntity<Void> post(
            @ApiParam(name = "connection", value = "The new connection") @RequestBody AddSubtopicToTopicCommand command) throws Exception {

        Topic topic = topicRepository.getByPublicId(command.topicid);
        Topic subtopic = topicRepository.getByPublicId(command.subtopicid);

        TopicSubtopic topicSubtopic = topic.addSubtopic(subtopic);
        topicSubtopicRepository.save(topicSubtopic);

        topicSubtopic.setRank(command.rank);
        if (command.primary) subtopic.setPrimaryParentTopic(topic);

        URI location = URI.create("/topic-subtopics/" + topicSubtopic.getPublicId());
        return ResponseEntity.created(location).build();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiOperation(value = "Removes a connection between a topic and a subtopic")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void delete(@PathVariable("id") URI id) throws Exception {
        TopicSubtopic topicSubtopic = topicSubtopicRepository.getByPublicId(id);
        topicSubtopic.getTopic().removeSubtopic(topicSubtopic.getSubtopic());
        topicSubtopicRepository.delete(topicSubtopic);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiOperation(value = "Updates a connection between a topic and a subtopic", notes = "Use to update which topic is primary to a subtopic or to alter sorting order")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void put(@PathVariable("id") URI id,
                    @ApiParam(name = "connection", value = "The updated connection") @RequestBody UpdateTopicSubtopicCommand command) throws Exception {
        TopicSubtopic topicSubtopic = topicSubtopicRepository.getByPublicId(id);
        topicSubtopic.setRank(command.rank);
        if (command.primary) {
            topicSubtopic.setPrimary(true);
        } else if (topicSubtopic.isPrimary() && !command.primary) {
            throw new PrimaryParentRequiredException();
        }
    }

    @PutMapping
    @ApiOperation(value = "Replaces a collection of topic subtopics connections")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void putTopicSubtopics(@ApiParam(name = "topic-subtopics", value = "A list of topic subtopic connections") @RequestBody AddSubtopicToTopicCommand[] commands) throws Exception {
        topicSubtopicRepository.deleteAll();
        for (AddSubtopicToTopicCommand command : commands) {
            post(command);
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
        public boolean primary;

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
            topicid = topicSubtopic.getTopic().getPublicId();
            subtopicid = topicSubtopic.getSubtopic().getPublicId();
            primary = topicSubtopic.isPrimary();
            rank = topicSubtopic.getRank();
        }
    }
}
