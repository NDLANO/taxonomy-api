package no.ndla.taxonomy.rest.v1.controllers;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.repositories.FilterRepository;
import no.ndla.taxonomy.repositories.RelevanceRepository;
import no.ndla.taxonomy.repositories.TopicFilterRepository;
import no.ndla.taxonomy.repositories.TopicRepository;
import no.ndla.taxonomy.services.PublicIdGeneratorService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(path = {"/v1/topic-filters"})
@Transactional
public class TopicFilters {
    private FilterRepository filterRepository;
    private TopicFilterRepository topicFilterRepository;
    private TopicRepository topicRepository;
    private RelevanceRepository relevanceRepository;
    private PublicIdGeneratorService publicIdGeneratorService;

    public TopicFilters(FilterRepository filterRepository, TopicRepository topicRepository,
                        TopicFilterRepository topicFilterRepository, RelevanceRepository relevanceRepository,
                        PublicIdGeneratorService publicIdGeneratorService) {
        this.filterRepository = filterRepository;
        this.topicRepository = topicRepository;
        this.topicFilterRepository = topicFilterRepository;
        this.relevanceRepository = relevanceRepository;
        this.publicIdGeneratorService = publicIdGeneratorService;
    }

    @PostMapping
    @ApiOperation(value = "Adds a filter to a topic")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public ResponseEntity<Void> post(@ApiParam(name = "topic filter", value = "The new topic filter") @RequestBody AddFilterToTopicCommand command) throws Exception {
        try {
            Filter filter = filterRepository.getByPublicId(command.filterId);
            Topic topic = topicRepository.getByPublicId(command.topicId);
            Relevance relevance = relevanceRepository.getByPublicId(command.relevanceId);

            TopicFilter topicFilter = topic.addFilter(filter, relevance, publicIdGeneratorService.getNext("urn:topic-filter"));
            topicFilterRepository.save(topicFilter);

            URI location = URI.create("/v1/topic-filters/" + topicFilter.getPublicId());
            return ResponseEntity.created(location).build();

        } catch (DataIntegrityViolationException e) {
            throw new DuplicateIdException("Topic " + command.topicId + " is already associated with filter " + command.filterId);
        }
    }

    @PutMapping("/{id}")
    @ApiOperation(value = "Updates a topic filter connection")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void put(@PathVariable("id") URI id, @ApiParam(name = "topic filter", value = "The updated topic filter", required = true) @RequestBody UpdateTopicFilterCommand command) throws Exception {
        TopicFilter topicFilter = topicFilterRepository.getByPublicId(id);
        Relevance relevance = relevanceRepository.getByPublicId(command.relevanceId);
        topicFilter.setRelevance(relevance);
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "Deletes a connection between a topic and a filter")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void delete(@ApiParam(name = "id", value = "The id of the connection to delete", required = true) @PathVariable String id) {
        TopicFilter topicFilter = topicFilterRepository.getByPublicId(URI.create(id));
        topicFilter.getTopic().removeFilter(topicFilter.getFilter());
        topicFilterRepository.deleteByPublicId(URI.create(id));
    }

    @GetMapping
    @ApiOperation("Gets all connections between topics and filters")
    public List<TopicFilterIndexDocument> index() throws Exception {
        List<TopicFilterIndexDocument> result = new ArrayList<>();
        topicFilterRepository.findAll().forEach(record -> result.add(new TopicFilterIndexDocument(record)));
        return result;
    }


    public static class AddFilterToTopicCommand {
        @JsonProperty
        @ApiModelProperty(required = true, value = "Topic id", example = "urn:topic:123")
        public URI topicId;

        @JsonProperty
        @ApiModelProperty(required = true, value = "Filter id", example = "urn:filter:234")
        public URI filterId;

        @JsonProperty
        @ApiModelProperty(required = true, value = "Relevance id", example = "urn:relevance:core")
        public URI relevanceId;
    }

    public static class UpdateTopicFilterCommand {
        public URI relevanceId;
    }

    @ApiModel("TopicFilterIndexDocument")
    public static class TopicFilterIndexDocument {
        @JsonProperty
        @ApiModelProperty(required = true, value = "Topic id", example = "urn:topic:123")
        public URI topicId;

        @JsonProperty
        @ApiModelProperty(required = true, value = "Filter id", example = "urn:filter:234")
        public URI filterId;

        @JsonProperty
        @ApiModelProperty(required = true, value = "Topic to filter connection id", example = "urn:topic-filter:12")
        public URI id;

        @JsonProperty
        @ApiModelProperty(required = true, value = "Relevance id", example = "urn:relevance:core")
        public URI relevanceId;

        public TopicFilterIndexDocument() {
        }

        public TopicFilterIndexDocument(TopicFilter topicFilter) {
            id = topicFilter.getPublicId();
            topicId = topicFilter.getTopic().getPublicId();
            filterId = topicFilter.getFilter().getPublicId();
            relevanceId = topicFilter.getRelevance().getPublicId();
        }
    }
}
