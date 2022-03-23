/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.task;

import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.repositories.NodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NodeFetcher extends VersionSchemaFetcher<Node> {

    @Autowired
    NodeRepository nodeRepository;

    @Override
    protected Node callInternal() {
        return nodeRepository.getByPublicId(this.publicId);
    }
}
