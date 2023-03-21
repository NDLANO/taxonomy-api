/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.dtos.nodes.searchapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.net.URI;
import java.util.List;
import java.util.Optional;

// NOTE: This will need to match `SearchableTaxonomyContext` in `search-api`
public record TaxonomyContextDTO(
        @JsonProperty @Schema(description = "The public-id of the node connected via content-uri") URI id,
        @JsonProperty @Schema(description = "The id of the root parent of the context") URI subjectId,
        @JsonProperty @Schema(description = "The name of the root parent of the context") LanguageField<String> subject,
        @JsonProperty @Schema(description = "The context path") String path,
        @JsonProperty @Schema(description = "A breadcrumb of the names of the context's path") LanguageField<List<String>> breadcrumbs,
        @JsonProperty @Schema(description = "Whether a 'standard'-article, 'topic-article'-article or a 'learningpath'") Optional<String> contextType,
        @JsonProperty @Schema(description = "Id of the relevance of the connection of the base") URI relevanceId,
        @JsonProperty @Schema(description = "Name of the relevance of the connection of the base") LanguageField<String> relevance,
        @JsonProperty @Schema(description = "Resource-types of the base") List<SearchableTaxonomyResourceType> resourceTypes,
        @JsonProperty @Schema(description = "List of all parent topic-ids") List<URI> parentTopicIds,
        @JsonProperty @Schema(description = "Whether the base connection is primary or not") boolean isPrimaryConnection) {
}
