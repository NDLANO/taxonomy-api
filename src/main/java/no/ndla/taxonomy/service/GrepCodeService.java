/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.GrepCode;

public interface GrepCodeService {
    GrepCode getOrCreateGrepCode(String code);
}
