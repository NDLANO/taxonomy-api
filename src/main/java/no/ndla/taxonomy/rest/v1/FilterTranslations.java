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
import no.ndla.taxonomy.domain.exceptions.NotFoundException;
import no.ndla.taxonomy.rest.NotFoundHttpResponseException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(path = {"/v1/filters/{id}/translations"})
@Transactional
@Deprecated(forRemoval = true)
public class FilterTranslations {
    public FilterTranslations() {
    }

    @GetMapping
    @ApiOperation("Gets all relevanceTranslations for a single filter")
    @Deprecated(forRemoval = true)
    public List<FilterTranslations.FilterTranslationIndexDocument> index(@PathVariable("id") URI id) {
        throw new NotFoundHttpResponseException("Filter was not found");
    }

    @GetMapping("/{language}")
    @ApiOperation("Gets a single translation for a single filter")
    @Deprecated(forRemoval = true)
    public FilterTranslations.FilterTranslationIndexDocument get(
            @PathVariable("id") URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb", required = true)
            @PathVariable("language") String language
    ) {
        throw new NotFoundHttpResponseException("Filter was not found");
    }

    @PutMapping("/{language}")
    @ApiOperation("Creates or updates a translation of a filter")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Deprecated(forRemoval = true)
    public void put(
            @PathVariable("id") URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb", required = true)
            @PathVariable("language") String language,
            @ApiParam(name = "filter", value = "The new or updated translation")
            @RequestBody FilterTranslations.UpdateFilterTranslationCommand command
    ) {
        throw new NotFoundHttpResponseException("Filter was not found");
    }

    @DeleteMapping("/{language}")
    @ApiOperation("Deletes a translation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Deprecated(forRemoval = true)
    public void delete(
            @PathVariable("id") URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb", required = true)
            @PathVariable("language") String language
    ) {
        throw new NotFoundHttpResponseException("Filter was not found");
    }


    @ApiModel("FilterTranslationIndexDocument")
    public static class FilterTranslationIndexDocument {
        @JsonProperty
        @ApiModelProperty(value = "The translated name of the filter", example = "Carpenter")
        public String name;

        @JsonProperty
        @ApiModelProperty(value = "ISO 639-1 language code", example = "en")
        public String language;
    }


    public static class UpdateFilterTranslationCommand {
        @JsonProperty
        @ApiModelProperty(value = "The translated name of the filter", example = "Carpenter")
        public String name;
    }
}
