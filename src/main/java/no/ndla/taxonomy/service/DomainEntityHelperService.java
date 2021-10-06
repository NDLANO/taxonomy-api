/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.Subject;
import no.ndla.taxonomy.domain.Topic;

import java.net.URI;

public interface DomainEntityHelperService {
    Subject getSubjectByPublicId(URI publicId);

    Topic getTopicByPublicId(URI publicId);

    Node getNodeByPublicId(URI publicId);
}
