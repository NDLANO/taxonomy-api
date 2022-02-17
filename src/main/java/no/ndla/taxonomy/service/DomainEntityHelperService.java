/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.EntityWithPath;
import no.ndla.taxonomy.domain.Node;

import java.net.URI;

public interface DomainEntityHelperService {
    Node getNodeByPublicId(URI publicId);

    EntityWithPath getEntityByPublicId(URI publicId);
}
