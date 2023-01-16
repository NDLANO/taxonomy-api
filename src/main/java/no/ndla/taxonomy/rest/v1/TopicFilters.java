/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import no.ndla.taxonomy.rest.NotFoundHttpResponseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping(path = { "/v1/topic-filters" })
@Transactional
@Deprecated(forRemoval = true)
public class TopicFilters {
    public TopicFilters() {
    }

    @PostMapping
    @Operation(summary = "Adds a filter to a topic", security = { @SecurityRequirement(name = "oauth") })
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Deprecated(forRemoval = true)
    public ResponseEntity<Void> post(
            @Parameter(name = "topic filter", description = "The new topic filter") @RequestBody AddFilterToTopicCommand command) {
        throw new NotFoundHttpResponseException("Endpoint deprecated");
    }

    @PutMapping("/{id}")
    @Operation(summary = "Updates a topic filter connection", security = { @SecurityRequirement(name = "oauth") })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Deprecated(forRemoval = true)
    public void put(@PathVariable("id") URI id,
            @Parameter(name = "topic filter", description = "The updated topic filter", required = true) @RequestBody UpdateTopicFilterCommand command) {
        throw new NotFoundHttpResponseException("Endpoint deprecated");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deletes a connection between a topic and a filter", security = {
            @SecurityRequirement(name = "oauth") })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Deprecated(forRemoval = true)
    public void delete(
            @Parameter(name = "id", description = "The id of the connection to delete", required = true) @PathVariable String id) {
        throw new NotFoundHttpResponseException("Endpoint deprecated");
    }

    @GetMapping
    @Operation(summary = "Gets all connections between topics and filters")
    @Deprecated(forRemoval = true)
    public List<TopicFilterIndexDocument> index() {
        return List.of();
    }

    public static class AddFilterToTopicCommand {
        @JsonProperty
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Topic id", example = "urn:topic:123")
        public URI topicId;

        @JsonProperty
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Filter id", example = "urn:filter:234")
        public URI filterId;

        @JsonProperty
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Relevance id", example = "urn:relevance:core")
        public URI relevanceId;
    }

    public static class UpdateTopicFilterCommand {
        public URI relevanceId;
    }

    @Schema(name = "TopicFilterIndexDocument")
    public static class TopicFilterIndexDocument {
        @JsonProperty
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Topic id", example = "urn:topic:123")
        public URI topicId;

        @JsonProperty
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Filter id", example = "urn:filter:234")
        public URI filterId;

        @JsonProperty
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Topic to filter connection id", example = "urn:topic-filter:12")
        public URI id;

        @JsonProperty
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Relevance id", example = "urn:relevance:core")
        public URI relevanceId;

        public TopicFilterIndexDocument() {
        }
    }
}
