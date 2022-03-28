/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.task;

import no.ndla.taxonomy.domain.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;

@Component
public class NodeFetcher extends VersionSchemaFetcher<Node> {

    @Autowired
    EntityManager entityManager;

    @Override
    protected Node callInternal() {
        EntityGraph entityGraph = entityManager.getEntityGraph("node-with-connections");
        return entityManager.createQuery("select n from Node n where n.publicId = :id", Node.class)
                .setParameter("id", this.publicId).setHint("javax.persistence.fetchgraph", entityGraph)
                .getSingleResult();
    }
}
