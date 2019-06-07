package no.ndla.taxonomy.rest.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.repositories.SubjectRepository;
import no.ndla.taxonomy.repositories.SubjectTopicRepository;
import no.ndla.taxonomy.repositories.TopicRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = {"/v1/subject-topics"})
@Transactional
public class SubjectTopics {
    private TopicRepository topicRepository;
    private SubjectTopicRepository subjectTopicRepository;
    private SubjectRepository subjectRepository;

    public SubjectTopics(SubjectRepository subjectRepository, TopicRepository topicRepository, SubjectTopicRepository subjectTopicRepository) {
        this.subjectRepository = subjectRepository;
        this.subjectTopicRepository = subjectTopicRepository;
        this.topicRepository = topicRepository;
    }


    @GetMapping
    @ApiOperation("Gets all connections between subjects and topics")
    public List<SubjectTopicIndexDocument> index() {
        return subjectTopicRepository
                .findAllIncludingSubjectAndTopic()
                .stream()
                .map(SubjectTopicIndexDocument::new)
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
            @ApiParam(name = "command", value = "The subject and topic getting connected. Use primary=true if primary connection for this topic.") @RequestBody AddTopicToSubjectCommand command) {

        Subject subject = subjectRepository.getByPublicId(command.subjectid);
        Topic topic = topicRepository.getByPublicId(command.topicid);

        SubjectTopic subjectTopic = subject.addTopic(topic);

        if (command.primary) {
            topic.setPrimarySubject(subject);
        }

        List<SubjectTopic> connectionsForSubject = subjectTopicRepository.findBySubject(subject);
        connectionsForSubject.sort(Comparator.comparingInt(SubjectTopic::getRank));
        if (command.rank == 0) {
            SubjectTopic highestRankingConnection = connectionsForSubject.get(connectionsForSubject.size() - 1);
            subjectTopic.setRank(highestRankingConnection.getRank() + 1);
        } else {
            List<SubjectTopic> rankedConnections = RankableConnectionUpdater.rank(connectionsForSubject, subjectTopic, command.rank);
            subjectTopicRepository.saveAll(rankedConnections);
        }

        subjectTopicRepository.save(subjectTopic);

        URI location = URI.create("/subject-topics/" + subjectTopic.getPublicId());
        return ResponseEntity.created(location).build();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiOperation("Removes a topic from a subject")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void delete(@PathVariable("id") URI id) {
        SubjectTopic subjectTopic = subjectTopicRepository.getByPublicId(id);
        subjectTopic.getSubject().removeTopic(subjectTopic.getTopic());
        subjectTopicRepository.deleteByPublicId(id);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiOperation(value = "Updates a connection between subject and topic", notes = "Use to update which subject is primary to a topic or to change sorting order.")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void put(@PathVariable("id") URI id,
                    @ApiParam(name = "connection", value = "updated subject/topic connection") @RequestBody UpdateSubjectTopicCommand command) {
        SubjectTopic subjectTopic = subjectTopicRepository.getByPublicId(id);
        Topic topic = subjectTopic.getTopic();
        Subject subject = subjectTopic.getSubject();

        List<SubjectTopic> existingConnections = subjectTopicRepository.findBySubject(subject);
        List<SubjectTopic> rankedConnections = RankableConnectionUpdater.rank(existingConnections, subjectTopic, command.rank);
        subjectTopicRepository.saveAll(rankedConnections);

        if (command.primary) {
            topic.setPrimarySubject(subject);
        } else if (subjectTopic.isPrimary() && !command.primary) {
            throw new PrimaryParentRequiredException();
        }
    }


    public static class AddTopicToSubjectCommand {
        @JsonProperty
        @ApiModelProperty(required = true, value = "Subject id", example = "urn:subject:123")
        public URI subjectid;

        @JsonProperty
        @ApiModelProperty(required = true, value = "Topic id", example = "urn:topic:234")
        public URI topicid;

        @JsonProperty
        @ApiModelProperty(value = "If this is the primary subject of this topic.", example = "true",
                notes = "The first subject added to a topic will always become primary, regardless of the value provided here.")
        public boolean primary;

        @JsonProperty
        @ApiModelProperty(value = "Order in which the topic should be sorted for the topic", example = "1")
        public int rank;
    }

    public static class UpdateSubjectTopicCommand {
        @JsonProperty
        @ApiModelProperty(required = true, value = "connection id", example = "urn:subject-topic:2")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "If true, set this subject as the primary subject for this topic", example = "true",
                notes = "This will replace any other primary subject for this topic. You must have one primary subject, so it is not allowed to set the currently primary subject to not be primary any more.")
        public boolean primary;

        @JsonProperty
        @ApiModelProperty(value = "Order in which the topic should be sorted for the subject", example = "1")
        public int rank;
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

        SubjectTopicIndexDocument() {
        }

        SubjectTopicIndexDocument(SubjectTopic subjectTopic) {
            id = subjectTopic.getPublicId();
            subjectid = subjectTopic.getSubject().getPublicId();
            topicid = subjectTopic.getTopic().getPublicId();
            primary = subjectTopic.isPrimary();
            rank = subjectTopic.getRank();
        }
    }
}
