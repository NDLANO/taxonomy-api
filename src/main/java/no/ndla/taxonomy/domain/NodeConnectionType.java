/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true)
public enum NodeConnectionType {
    BRANCH,
    LINK
}
