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
// spotless:off
@Schema(name = "TaxonomyContext")
public record TaxonomyContextDTO(
        @JsonProperty @Schema(description = "The publicId of the node connected via content-uri") URI publicId,
        @JsonProperty @Schema(description = "The publicId of the root parent of the context") URI rootId,
        @JsonProperty @Schema(description = "The name of the root parent of the context") LanguageFieldDTO<String> root,
        @JsonProperty @Schema(description = "The context path") String path,
        @JsonProperty @Schema(description = "A breadcrumb of the names of the context's parents") LanguageFieldDTO<List<String>> breadcrumbs,
        @JsonProperty @Schema(description = "Whether a 'standard'-article, 'topic-article'-article or a 'learningpath'") Optional<String> contextType,
        @JsonProperty @Schema(description = "Id of the relevance of the parent connection") URI relevanceId,
        @JsonProperty @Schema(description = "Name of the relevance of the parent connection") LanguageFieldDTO<String> relevance,
        @JsonProperty @Schema(description = "Resource-types of the node") List<SearchableTaxonomyResourceType> resourceTypes,
        @JsonProperty @Schema(description = "List of all parent ids") List<URI> parentIds,
        @JsonProperty @Schema(description = "List of all parent contextIds") List<String> parentContextIds,
        @JsonProperty @Schema(description = "Whether the parent connection is primary or not") boolean isPrimary,
        @JsonProperty @Schema(description = "Whether the parent connection is marked as active") boolean isActive,
        @JsonProperty @Schema(description = "Whether the parent connection is visible or not") boolean isVisible,
        @JsonProperty @Schema(description = "Unique id of context based on root + parent connection") String contextId,
        @JsonProperty @Schema(description = "The rank of the parent connection object") int rank,
        @JsonProperty @Schema(description = "The id of the parent connection object") String connectionId,
        @JsonProperty @Schema(description = "Pretty-url of this particular context") Optional<String> url) {
// spotless:on
}
