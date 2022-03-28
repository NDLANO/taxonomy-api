/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.task;

import no.ndla.taxonomy.domain.NodeConnection;
import no.ndla.taxonomy.domain.NodeResource;

import java.util.Set;

public abstract class VersionSchemaUpdater<TYPE> extends VersionSchemaTask<TYPE> {
    protected TYPE type;
    protected Set<NodeConnection> children;
    protected Set<NodeResource> resources;

    public void setType(TYPE type) {
        this.type = type;
    }

    public void setChildren(Set<NodeConnection> children) {
        this.children = children;
    }

    public void setResources(Set<NodeResource> resources) {
        this.resources = resources;
    }
}
