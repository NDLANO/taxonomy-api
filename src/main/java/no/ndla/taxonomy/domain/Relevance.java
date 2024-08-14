/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import java.net.URI;

public enum Relevance {
    CORE,
    SUPPLEMENTARY;

    public URI getPublicId() {
        return RelevanceStore.fromEnum(this).id;
    }
}
