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

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping(path = { "/v1/resource-filters" })
@Deprecated(forRemoval = true)
public class ResourceFilters {

    public ResourceFilters() {
    }

    @PostMapping
    @Operation(summary = "Adds a filter to a resource", security = { @SecurityRequirement(name = "oauth") })
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Deprecated(forRemoval = true)
    public ResponseEntity<Void> post(
            @Parameter(name = "resource filter", description = "The new resource filter") @RequestBody ResourceFiltersPOST command) {
        throw new NotFoundHttpResponseException("Endpoint deprecated");
    }

    @PutMapping("/{id}")
    @Operation(summary = "Updates a resource filter connection", security = { @SecurityRequirement(name = "oauth") })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Deprecated(forRemoval = true)
    public void put(@PathVariable("id") URI id,
            @Parameter(name = "resource filter", description = "The updated resource filter", required = true) @RequestBody ResourceFilterPUT command) {
        throw new NotFoundHttpResponseException("Endpoint deprecated");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deletes a resource filter connection", security = { @SecurityRequirement(name = "oauth") })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Deprecated(forRemoval = true)
    public void delete(
            @Parameter(name = "id", description = "The id of the connection to delete", required = true) @PathVariable URI id) {
        throw new NotFoundHttpResponseException("Endpoint deprecated");
    }

    @GetMapping("/{id}")
    @Deprecated(forRemoval = true)
    public ResourceFilterDTO get(
            @Parameter(name = "id", description = "The id of the connection to get", required = true) @PathVariable URI id) {
        throw new NotFoundHttpResponseException("Endpoint deprecated");
    }

    @GetMapping
    @Operation(summary = "Gets all connections between resources and filters")
    @Deprecated(forRemoval = true)
    public List<ResourceFilterDTO> index() {
        return List.of();
    }

    public static class ResourceFiltersPOST {
        @JsonProperty
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Resource id", example = "urn:resource:123")
        public URI resourceId;

        @JsonProperty
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Filter id", example = "urn:filter:234")
        public URI filterId;

        @JsonProperty
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Relevance id", example = "urn:relevance:core")
        public URI relevanceId;
    }

    public static class ResourceFilterPUT {
        public URI relevanceId;
    }

    @Schema(name = "ResourceFilter")
    public static class ResourceFilterDTO {
        @JsonProperty
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Resource id", example = "urn:resource:123")
        public URI resourceId;

        @JsonProperty
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Filter id", example = "urn:filter:234")
        public URI filterId;

        @JsonProperty
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Resource to filter connection id", example = "urn:resource-filter:12")
        public URI id;

        @JsonProperty
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Relevance id", example = "urn:relevance:core")
        public URI relevanceId;

        public ResourceFilterDTO() {
        }
    }
}
