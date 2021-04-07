package no.ndla.taxonomy.rest.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.Relevance;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.domain.TopicSubtopic;
import no.ndla.taxonomy.repositories.RelevanceRepository;
import no.ndla.taxonomy.repositories.TopicRepository;
import no.ndla.taxonomy.repositories.TopicSubtopicRepository;
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
@RequestMapping(path = {"/v1/node-subnodes"})
@Transactional
public class NodeSubnodes {
    private final TopicRepository topicRepository;
    private final TopicSubtopicRepository topicSubtopicRepository;
    private final EntityConnectionService connectionService;
    private final RelevanceRepository relevanceRepository;

    public NodeSubnodes(
            TopicRepository topicRepository,
            TopicSubtopicRepository topicSubtopicRepository,
            EntityConnectionService connectionService,
            RelevanceRepository relevanceRepository
    ) {
        this.topicRepository = topicRepository;
        this.topicSubtopicRepository = topicSubtopicRepository;
        this.connectionService = connectionService;
        this.relevanceRepository = relevanceRepository;
    }

    @GetMapping
    @ApiOperation(value = "Gets all connections between nodes and subnodes")
    public List<NodeSubnodeIndexDocument> index() {
        return topicSubtopicRepository
                .findAllIncludingTopicAndSubtopic()
                .stream()
                .map(NodeSubnodeIndexDocument::new)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "Gets a single connection between a node and a subnode")
    public NodeSubnodeIndexDocument get(@PathVariable("id") URI id) {
        TopicSubtopic topicSubtopic = topicSubtopicRepository.getByPublicId(id);
        return new NodeSubnodeIndexDocument(topicSubtopic);
    }

    @PostMapping
    @ApiOperation(value = "Adds a subnode to a node")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public ResponseEntity<Void> post(
            @ApiParam(name = "connection", value = "The new connection") @RequestBody AddSubnodeToNodeCommand command) {

        Topic topic = topicRepository.getByPublicId(command.nodeid);
        Topic subtopic = topicRepository.getByPublicId(command.subnodeid);
        Relevance relevance = command.relevanceId != null ? relevanceRepository.getByPublicId(command.relevanceId) : null;

        final var topicSubtopic = connectionService.connectTopicSubtopic(topic, subtopic, relevance, command.rank == 0 ? null : command.rank);

        URI location = URI.create("/node-subnodes/" + topicSubtopic.getPublicId());
        return ResponseEntity.created(location).build();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiOperation(value = "Removes a connection between a node and a subnode")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void delete(@PathVariable("id") URI id) {
        connectionService.disconnectTopicSubtopic(topicSubtopicRepository.getByPublicId(id));
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiOperation(value = "Updates a connection between a node and a subnode", notes = "Use to update which node is primary to a subnode or to alter sorting order")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void put(@PathVariable("id") URI id,
                    @ApiParam(name = "connection", value = "The updated connection") @RequestBody UpdateNodeSubnodeCommand command) {
        final var topicSubtopic = topicSubtopicRepository.getByPublicId(id);
        Relevance relevance = command.relevanceId != null ? relevanceRepository.getByPublicId(command.relevanceId) : null;

        connectionService.updateTopicSubtopic(topicSubtopic, relevance, command.rank > 0 ? command.rank : null);
    }

    public static class AddSubnodeToNodeCommand {
        @JsonProperty
        @ApiModelProperty(required = true, value = "Topic id", example = "urn:topic:234")
        public URI nodeid;

        @JsonProperty
        @ApiModelProperty(required = true, value = "Subtopic id", example = "urn:topic:234")
        public URI subnodeid;

        @JsonProperty
        @ApiModelProperty(value = "Order in which to sort the subtopic for the topic", example = "1")
        public int rank;

        @JsonProperty
        @ApiModelProperty(value = "Relevance id", example = "urn:relevance:core")
        public URI relevanceId;
    }

    public static class UpdateNodeSubnodeCommand {
        @JsonProperty
        @ApiModelProperty(value = "Connection id", example = "urn:topic-has-subtopics:345")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "Order in which subnode is sorted for the node", example = "1")
        public int rank;

        @JsonProperty
        @ApiModelProperty(value = "Relevance id", example = "urn:relevance:core")
        public URI relevanceId;
    }

    public static class NodeSubnodeIndexDocument {
        @JsonProperty
        @ApiModelProperty(value = "Node id", example = "urn:topic:234")
        public URI nodeid;

        @JsonProperty
        @ApiModelProperty(value = "Subnode id", example = "urn:topic:234")
        public URI subnodeid;

        @JsonProperty
        @ApiModelProperty(value = "Connection id", example = "urn:topic-has-subtopics:345")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "Order in which subtopic is sorted for the topic", example = "1")
        public int rank;

        @JsonProperty
        @ApiModelProperty(value = "Relevance id", example = "urn:relevance:core")
        public URI relevanceId;

        NodeSubnodeIndexDocument() {
        }

        NodeSubnodeIndexDocument(TopicSubtopic topicSubtopic) {
            id = topicSubtopic.getPublicId();
            topicSubtopic.getTopic().ifPresent(topic -> nodeid = topic.getPublicId());
            topicSubtopic.getSubtopic().ifPresent(subtopic -> subnodeid = subtopic.getPublicId());
            relevanceId = topicSubtopic.getRelevance().map(Relevance::getPublicId).orElse(null);
            rank = topicSubtopic.getRank();
        }
    }
}
