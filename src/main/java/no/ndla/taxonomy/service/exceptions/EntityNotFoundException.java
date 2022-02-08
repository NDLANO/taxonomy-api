/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.exceptions;

import java.util.UUID;

public class EntityNotFoundException extends Exception {
    public EntityNotFoundException(String id) {
        super("Entity not found: " + id);
    }

    public EntityNotFoundException(UUID id) {
        this(id != null ? id.toString() : "null");
    }
}
