/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.task;

import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeConnection;
import no.ndla.taxonomy.domain.NodeResource;
import no.ndla.taxonomy.repositories.NodeConnectionRepository;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.repositories.NodeResourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class NodeUpdater extends VersionSchemaUpdater<Node> {

    @Autowired
    NodeRepository nodeRepository;

    @Autowired
    NodeConnectionRepository nodeConnectionRepository;

    @Override
    protected Node callInternal() {
        Node updated;
        Optional<Node> maybeNode = nodeRepository.findFirstByPublicId(this.type.getPublicId());
        if (maybeNode.isPresent()) {
            this.type.setId(maybeNode.get().getId());
            updated = nodeRepository.save(this.type);
        } else {
            updated = nodeRepository.save(this.type);
        }
        for (NodeConnection connection : this.children) {
            Optional<NodeConnection> maybeConnection = nodeConnectionRepository
                    .findFirstByPublicId(connection.getPublicId());
            if (maybeConnection.isPresent()) {
                connection.setId(maybeConnection.get().getId());
                nodeConnectionRepository.save(maybeConnection.get());
            } else {
                connection.setId(null);
                nodeConnectionRepository.save(NodeConnection.create(this.type, connection.getChild().get()));
            }
        }
        return updated;
    }
}
