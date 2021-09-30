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
import no.ndla.taxonomy.domain.Relevance;
import no.ndla.taxonomy.domain.Resource;
import no.ndla.taxonomy.domain.exceptions.DuplicateIdException;
import no.ndla.taxonomy.repositories.RelevanceRepository;
import no.ndla.taxonomy.repositories.ResourceRepository;
import no.ndla.taxonomy.rest.NotFoundHttpResponseException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = {"/v1/resource-filters"})
@Transactional
public class ResourceFilters {
    private final ResourceRepository resourceRepository;
    private final RelevanceRepository relevanceRepository;

    public ResourceFilters(ResourceRepository resourceRepository, RelevanceRepository relevanceRepository) {
        this.resourceRepository = resourceRepository;
        this.relevanceRepository = relevanceRepository;
    }

    @PostMapping
    @ApiOperation(value = "Adds a filter to a resource")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public ResponseEntity<Void> post(@ApiParam(name = "resource filter", value = "The new resource filter") @RequestBody AddFilterToResourceCommand command) {
        throw new NotFoundHttpResponseException("Filter was not found");
    }

    @PutMapping("/{id}")
    @ApiOperation(value = "Updates a resource filter connection")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void put(@PathVariable("id") URI id, @ApiParam(name = "resource filter", value = "The updated resource filter", required = true) @RequestBody UpdateResourceFilterCommand command) {
        throw new NotFoundHttpResponseException("Filter was not found");
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void delete(@ApiParam(name = "id", value = "The id of the connection to delete", required = true) @PathVariable URI id) {
        throw new NotFoundHttpResponseException("Filter was not found");
    }

    @GetMapping("/{id}")
    public ResourceFilterIndexDocument get(@ApiParam(name = "id", value = "The id of the connection to get", required = true) @PathVariable URI id) {
        throw new NotFoundHttpResponseException("Filter was not found");
    }

    @GetMapping
    @ApiOperation("Gets all connections between resources and filters")
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
        @ApiModelProperty(required = true, value = "Resource to filter connection id", example = "urn:resource-filter:12")
        public URI id;

        @JsonProperty
        @ApiModelProperty(required = true, value = "Relevance id", example = "urn:relevance:core")
        public URI relevanceId;

        public ResourceFilterIndexDocument() {
        }
    }
}
