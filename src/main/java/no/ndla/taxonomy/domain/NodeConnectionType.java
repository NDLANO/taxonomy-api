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
    BRANCH("parent", "child"),
    LINK("referrer", "target");

    private final String source;
    private final String target;

    NodeConnectionType(String source, String target) {
        this.source = source;
        this.target = target;
    }

    public String sourceString() {
        return source;
    }

    public String targetString() {
        return target;
    }
}
