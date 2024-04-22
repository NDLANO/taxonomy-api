/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Optional;

/**
 * Identifies a unique context for any node. A context is a position for a node in the structure, identified by root
 * node and parent-connection
 *
 * @param rootId           The publicId of the node at the root of the context.
 * @param rootName         The name of the root.
 * @param path             The path for this connection.
 * @param breadcrumbs      Breadcrumbs corresponding with the path.
 * @param contextType      Type resource. Fetched from node.
 * @param parentIds        Parents ids.
 * @param parentContextIds Parents context ids.
 * @param isVisible        Metadata from node.
 * @param isActive         Metadata from node. True if subjectCategory is active or otherResources.
 * @param isPrimary        Is this context marked as primary. From nodeConnection.
 * @param relevanceId      ID of relevance. From nodeConnection.
 * @param contextId        Hash of root publicId + nodeConnection publicId. Unique for this context.
 * @param rank             The rank of the context. From nodeConnection.
 * @param connectionId     The id of the connection. From nodeConnection.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TaxonomyContext(
        String rootId,
        LanguageField<String> rootName,
        String path,
        LanguageField<List<String>> breadcrumbs,
        Optional<String> contextType,
        List<String> parentIds,
        List<String> parentContextIds,
        boolean isVisible,
        boolean isActive,
        boolean isPrimary,
        String relevanceId,
        String contextId,
        int rank,
        String connectionId) {}
