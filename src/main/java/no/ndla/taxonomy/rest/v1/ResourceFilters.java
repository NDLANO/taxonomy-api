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
@RequestMapping(path = {"/v1/resource-filters"})
@Transactional
@Deprecated(forRemoval = true)
public class ResourceFilters {

    public ResourceFilters() {}

    @PostMapping
    @ApiOperation(value = "Adds a filter to a resource")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Deprecated(forRemoval = true)
    public ResponseEntity<Void> post(
            @ApiParam(name = "resource filter", value = "The new resource filter") @RequestBody
                    AddFilterToResourceCommand command) {
        throw new NotFoundHttpResponseException("Endpoint deprecated");
    }

    @PutMapping("/{id}")
    @ApiOperation(value = "Updates a resource filter connection")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Deprecated(forRemoval = true)
    public void put(
            @PathVariable("id") URI id,
            @ApiParam(
                            name = "resource filter",
                            value = "The updated resource filter",
                            required = true)
                    @RequestBody
                    UpdateResourceFilterCommand command) {
        throw new NotFoundHttpResponseException("Endpoint deprecated");
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Deprecated(forRemoval = true)
    public void delete(
            @ApiParam(name = "id", value = "The id of the connection to delete", required = true)
                    @PathVariable
                    URI id) {
        throw new NotFoundHttpResponseException("Endpoint deprecated");
    }

    @GetMapping("/{id}")
    @Deprecated(forRemoval = true)
    public ResourceFilterIndexDocument get(
            @ApiParam(name = "id", value = "The id of the connection to get", required = true)
                    @PathVariable
                    URI id) {
        throw new NotFoundHttpResponseException("Endpoint deprecated");
    }

    @GetMapping
    @ApiOperation("Gets all connections between resources and filters")
    @Deprecated(forRemoval = true)
    public List<ResourceFilterIndexDocument> index() {
        return List.of();
    }

    public static class AddFilterToResourceCommand {
        @JsonProperty
        @ApiModelProperty(required = true, value = "Resource id", example = "urn:resource:123")
        public URI resourceId;

        @JsonProperty
        @ApiModelProperty(required = true, value = "Filter id", example = "urn:filter:234")
        public URI filterId;

        @JsonProperty
        @ApiModelProperty(required = true, value = "Relevance id", example = "urn:relevance:core")
        public URI relevanceId;
    }

    public static class UpdateResourceFilterCommand {
        public URI relevanceId;
    }

    @ApiModel("ResourceFilterIndexDocument")
    public static class ResourceFilterIndexDocument {
        @JsonProperty
        @ApiModelProperty(required = true, value = "Resource id", example = "urn:resource:123")
        public URI resourceId;

        @JsonProperty
        @ApiModelProperty(required = true, value = "Filter id", example = "urn:filter:234")
        public URI filterId;

        @JsonProperty
        @ApiModelProperty(
                required = true,
                value = "Resource to filter connection id",
                example = "urn:resource-filter:12")
        public URI id;

        @JsonProperty
        @ApiModelProperty(required = true, value = "Relevance id", example = "urn:relevance:core")
        public URI relevanceId;

        public ResourceFilterIndexDocument() {}
    }
}
