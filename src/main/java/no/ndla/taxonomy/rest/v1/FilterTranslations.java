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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping(path = { "/v1/filters/{id}/translations" })
@Transactional
@Deprecated(forRemoval = true)
public class FilterTranslations {
    public FilterTranslations() {
    }

    @GetMapping
    @Operation(summary = "Gets all relevanceTranslations for a single filter")
    @Deprecated(forRemoval = true)
    public List<FilterTranslations.FilterTranslationIndexDocument> index(@PathVariable("id") URI id) {
        throw new NotFoundHttpResponseException("Filter was not found");
    }

    @GetMapping("/{language}")
    @Operation(summary = "Gets a single translation for a single filter")
    @Deprecated(forRemoval = true)
    public FilterTranslations.FilterTranslationIndexDocument get(@PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb", required = true) @PathVariable("language") String language) {
        throw new NotFoundHttpResponseException("Filter was not found");
    }

    @PutMapping("/{language}")
    @Operation(summary = "Creates or updates a translation of a filter", security = {
            @SecurityRequirement(name = "oauth") })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Deprecated(forRemoval = true)
    public void put(@PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb", required = true) @PathVariable("language") String language,
            @Parameter(name = "filter", description = "The new or updated translation") @RequestBody FilterTranslations.UpdateFilterTranslationCommand command) {
        throw new NotFoundHttpResponseException("Filter was not found");
    }

    @DeleteMapping("/{language}")
    @Operation(summary = "Deletes a translation", security = { @SecurityRequirement(name = "oauth") })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Deprecated(forRemoval = true)
    public void delete(@PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb", required = true) @PathVariable("language") String language) {
        throw new NotFoundHttpResponseException("Filter was not found");
    }

    @Schema(name = "FilterTranslationIndexDocument")
    public static class FilterTranslationIndexDocument {
        @JsonProperty
        @Schema(description = "The translated name of the filter", example = "Carpenter")
        public String name;

        @JsonProperty
        @Schema(description = "ISO 639-1 language code", example = "en")
        public String language;
    }

    public static class UpdateFilterTranslationCommand {
        @JsonProperty
        @Schema(description = "The translated name of the filter", example = "Carpenter")
        public String name;
    }
}
