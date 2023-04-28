/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import no.ndla.taxonomy.rest.NotFoundHttpResponseException;
import no.ndla.taxonomy.service.exceptions.InvalidArgumentServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@Deprecated(forRemoval = true)
public class Filters extends ObsoleteCrudController {
    public Filters() {
    }

    @GetMapping("/v1/filters")
    @Operation(summary = "Gets all filters")
    @Deprecated(forRemoval = true)
    public List<Object> index(
            @Parameter(description = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", required = false, defaultValue = "") String language) {
        return List.of();
    }

    @GetMapping("/v1/filters/{id}")
    @Operation(summary = "Gets a single filter", description = "Default language will be returned if desired language not found or if parameter is omitted.")
    @Deprecated(forRemoval = true)
    public Object get(@PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", required = false, defaultValue = "") String language) {
        throw new NotFoundHttpResponseException("Filter was not found");
    }

    @PostMapping("/v1/filters")
    @Operation(summary = "Creates a new filter", security = { @SecurityRequirement(name = "oauth") })
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Deprecated(forRemoval = true)
    public ResponseEntity<Void> post(
            @Parameter(name = "filter", description = "The new filter") @RequestBody FilterDTO command) {
        throw new InvalidArgumentServiceException("Create filter's disabled");
    }

    @PutMapping("/v1/filters/{id}")
    @Operation(summary = "Updates a filter", security = { @SecurityRequirement(name = "oauth") })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Deprecated(forRemoval = true)
    public void put(@PathVariable("id") URI id,
            @Parameter(name = "filter", description = "The updated filter") @RequestBody FilterDTO command) {
        throw new NotFoundHttpResponseException("Filter was not found");
    }

    @DeleteMapping("/v1/filters/{id}")
    @Operation(summary = "Delete a single filter by ID", security = { @SecurityRequirement(name = "oauth") })
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Deprecated(forRemoval = true)
    public void delete(@PathVariable("id") URI id) {
        throw new NotFoundHttpResponseException("Filter was not found");
    }

    @GetMapping("/v1/subjects/{subjectId}/filters")
    @Operation(summary = "Gets all filters for a subject", tags = { "subjects" })
    @Deprecated(forRemoval = true)
    public List<Object> getFiltersBySubjectId(@PathVariable("subjectId") URI subjectId,
            @Parameter(description = "ISO-639-1 language code", example = "nb") String language) {
        return List.of();
    }

    @GetMapping("/v1/resources/{resourceId}/filters")
    @Operation(summary = "Gets all filters associated with this resource", tags = { "resources" })
    @Deprecated(forRemoval = true)
    public List<Object> getFiltersByResourceId(@PathVariable("resourceId") URI resourceId,
            @Parameter(description = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", required = false, defaultValue = "") String language) {
        return List.of();
    }

    @Override
    protected String getLocation() {
        return "/v1/filters";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(name = "Filter")
    public class FilterDTO {
    }
}
