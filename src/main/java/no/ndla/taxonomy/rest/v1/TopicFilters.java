/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.rest.NotFoundHttpResponseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping(path = {"/v1/topic-filters"})
@Transactional
@Deprecated(forRemoval = true)
public class TopicFilters {
    public TopicFilters() {
    }

    @PostMapping
    @ApiOperation(value = "Adds a filter to a topic")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Deprecated(forRemoval = true)
    public ResponseEntity<Void> post(@ApiParam(name = "topic filter", value = "The new topic filter") @RequestBody AddFilterToTopicCommand command) {
        throw new NotFoundHttpResponseException("Endpoint deprecated");
    }

    @PutMapping("/{id}")
    @ApiOperation(value = "Updates a topic filter connection")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Deprecated(forRemoval = true)
    public void put(@PathVariable("id") URI id, @ApiParam(name = "topic filter", value = "The updated topic filter", required = true) @RequestBody UpdateTopicFilterCommand command) {
        throw new NotFoundHttpResponseException("Endpoint deprecated");
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "Deletes a connection between a topic and a filter")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Deprecated(forRemoval = true)
    public void delete(@ApiParam(name = "id", value = "The id of the connection to delete", required = true) @PathVariable String id) {
        throw new NotFoundHttpResponseException("Endpoint deprecated");
    }

    @GetMapping
    @ApiOperation("Gets all connections between topics and filters")
    @Deprecated(forRemoval = true)
    public List<TopicFilterIndexDocument> index() {
        return List.of();
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
    }
}
