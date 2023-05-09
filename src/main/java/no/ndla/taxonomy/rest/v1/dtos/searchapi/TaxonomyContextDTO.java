/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.dtos.searchapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.net.URI;
import java.util.List;
import java.util.Optional;

// NOTE: This will need to match `SearchableTaxonomyContext` in `search-api`
@Schema(name = "TaxonomyContext")
public record TaxonomyContextDTO(
        @JsonProperty @Schema(description = "The publicId of the node connected via content-uri") URI publicId,
        @JsonProperty @Schema(description = "The publicId of the root parent of the context") URI rootId,
        @JsonProperty @Schema(description = "The name of the root parent of the context") LanguageFieldDTO<String> root,
        @JsonProperty @Schema(description = "The context path") String path,
        @JsonProperty @Schema(description = "A breadcrumb of the names of the context's path") LanguageFieldDTO<List<String>> breadcrumbs,
        @JsonProperty @Schema(description = "Whether a 'standard'-article, 'topic-article'-article or a 'learningpath'") Optional<String> contextType,
        @JsonProperty @Schema(description = "Id of the relevance of the connection of the base") Optional<URI> relevanceId,
        @JsonProperty @Schema(description = "Name of the relevance of the connection of the base") LanguageFieldDTO<String> relevance,
        @JsonProperty @Schema(description = "Resource-types of the base") List<SearchableTaxonomyResourceType> resourceTypes,
        @JsonProperty @Schema(description = "List of all parent topic-ids") List<URI> parentIds,
        @JsonProperty @Schema(description = "Whether the base connection is primary or not") boolean isPrimary,
        @JsonProperty @Schema(description = "Whether the base connection is marked as active subject") boolean isActive,
        @JsonProperty @Schema(description = "Whether the base connection is visible or not") boolean isVisible,
        @JsonProperty @Schema(description = "Unique id of context based on root + connection") String contextId) {

    @JsonProperty
    @Deprecated
    public Optional<URI> id() {
        return Optional.of(publicId());
    }

    @JsonProperty
    @Deprecated
    public Optional<URI> subjectId() {
        return Optional.of(rootId());
    }

    @JsonProperty
    @Deprecated
    public Optional<LanguageFieldDTO<String>> subject() {
        return Optional.of(root());
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Deprecated
    public Optional<List<URI>> parentTopicIds() {
        return Optional.of(parentIds());
    }

    @JsonProperty("isPrimaryConnection")
    @Deprecated
    public Optional<Boolean> isPrimaryConnection() {
        return Optional.of(isPrimary());
    }

}
