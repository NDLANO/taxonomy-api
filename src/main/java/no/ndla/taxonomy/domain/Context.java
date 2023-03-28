/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import java.util.List;
import java.util.Optional;

public record Context(String rootId, LanguageField<String> rootName, String path,
        LanguageField<List<String>> breadcrumbs, String contextType, Optional<String> parentId, boolean isVisible,
        boolean isPrimary, String relevanceId, String contextId) {
}
