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
@RequestMapping(path = { "/v1/topic-resourcetypes" })
@Deprecated(forRemoval = true)
public class TopicsWithResourceTypes {

    public TopicsWithResourceTypes() {
    }

    @PostMapping
    @Operation(summary = "Adds a resource type to a topic", security = { @SecurityRequirement(name = "oauth") })
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Deprecated(forRemoval = true)
    public ResponseEntity<Void> post(
            @Parameter(name = "connection", description = "The new resource/resource type connection") @RequestBody CreateTopicResourceTypeCommand command) {
        throw new NotFoundHttpResponseException("Endpoint deprecated");
    }

    @DeleteMapping({ "/{id}" })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Removes a resource type from a topic", security = { @SecurityRequirement(name = "oauth") })
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Deprecated(forRemoval = true)
    public void delete(@PathVariable("id") URI id) {
        throw new NotFoundHttpResponseException("Endpoint deprecated");
    }

    @GetMapping
    @Operation(summary = "Gets all connections between topics and resource types")
    @Deprecated(forRemoval = true)
    public List<TopicResourceTypeIndexDocument> index() {
        return List.of();
    }

    @GetMapping({ "/{id}" })
    @Operation(summary = "Gets a single connection between topic and resource type")
    @Deprecated(forRemoval = true)
    public TopicResourceTypeIndexDocument get(@PathVariable("id") URI id) {
        throw new NotFoundHttpResponseException("Endpoint deprecated");
    }

    public static class CreateTopicResourceTypeCommand {
        @JsonProperty
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Topic id", example = "urn:topic:123")
        URI topicId;

        @JsonProperty
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Resource type id", example = "urn:resourcetype:234")
        URI resourceTypeId;
    }

    @Schema(name = "ResourceTypeIndexDocument")
    public static class TopicResourceTypeIndexDocument {
        @JsonProperty
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Topic type id", example = "urn:topic:123")
        URI topicId;

        @JsonProperty
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Resource type id", example = "urn:resourcetype:234")
        URI resourceTypeId;

        @JsonProperty
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Resource to resource type connection id", example = "urn:resource-has-resourcetypes:12")
        URI id;
    }
}
