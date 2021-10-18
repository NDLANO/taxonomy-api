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
@RequestMapping(path = { "/v1/topic-resourcetypes" })
@Transactional
@Deprecated(forRemoval = true)
public class TopicsWithResourceTypes {

    public TopicsWithResourceTypes() {
    }

    @PostMapping
    @ApiOperation(value = "Adds a resource type to a topic")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Deprecated(forRemoval = true)
    public ResponseEntity<Void> post(
            @ApiParam(name = "connection", value = "The new resource/resource type connection") @RequestBody CreateTopicResourceTypeCommand command) {
        throw new NotFoundHttpResponseException("Endpoint deprecated");
    }

    @DeleteMapping({ "/{id}" })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiOperation("Removes a resource type from a topic")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Deprecated(forRemoval = true)
    public void delete(@PathVariable("id") URI id) {
        throw new NotFoundHttpResponseException("Endpoint deprecated");
    }

    @GetMapping
    @ApiOperation("Gets all connections between topics and resource types")
    @Deprecated(forRemoval = true)
    public List<TopicResourceTypeIndexDocument> index() {
        return List.of();
    }

    @GetMapping({ "/{id}" })
    @ApiOperation("Gets a single connection between topic and resource type")
    @Deprecated(forRemoval = true)
    public TopicResourceTypeIndexDocument get(@PathVariable("id") URI id) {
        throw new NotFoundHttpResponseException("Endpoint deprecated");
    }

    public static class CreateTopicResourceTypeCommand {
        @JsonProperty
        @ApiModelProperty(required = true, value = "Topic id", example = "urn:topic:123")
        URI topicId;

        @JsonProperty
        @ApiModelProperty(required = true, value = "Resource type id", example = "urn:resourcetype:234")
        URI resourceTypeId;
    }

    @ApiModel("ResourceTypeIndexDocument")
    public static class TopicResourceTypeIndexDocument {
        @JsonProperty
        @ApiModelProperty(required = true, value = "Topic type id", example = "urn:topic:123")
        URI topicId;

        @JsonProperty
        @ApiModelProperty(required = true, value = "Resource type id", example = "urn:resourcetype:234")
        URI resourceTypeId;

        @JsonProperty
        @ApiModelProperty(required = true, value = "Resource to resource type connection id", example = "urn:resource-has-resourcetypes:12")
        URI id;
    }
}
