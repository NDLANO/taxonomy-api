/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.EntityWithPath;
import no.ndla.taxonomy.domain.SortableResourceConnection;
import no.ndla.taxonomy.domain.TopicResource;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

public class ResourceTreeSortable<T extends EntityWithPath> implements TreeSorter.Sortable {
    private int rank;
    private int id;
    private int parentId;
    private SortableResourceConnection<T> resourceConnection;
    private String parentType;
    private String type;

    public ResourceTreeSortable(SortableResourceConnection<T> resourceConnection) {
        this.id = resourceConnection.getResource().orElseThrow(() -> new IllegalArgumentException("Resource not set")).getId();
        this.parentId = resourceConnection.getParent().orElseThrow(() -> new IllegalArgumentException("Parent not set")).getId();
        this.rank = resourceConnection.getRank();
        this.resourceConnection = resourceConnection;
        this.type = "resource";
        this.parentType = "topic";
    }

    public ResourceTreeSortable(String type, String parentType, int id, int parentId, int rank) {
        this.id = id;
        this.parentId = parentId;
        this.rank = rank;
        this.type = type;
        this.parentType = parentType;
    }


    @Override
    public int getSortableRank() {
        // Sort subject-topic relations first, then topic-resource, then topic-topic

        if (type.equals("resource")) {
            return rank - 10000;
        }

        if (type.equals("topic") && parentType.equals("topic")) {
            return rank - 1000;
        }

        return rank;
    }

    @Override
    public URI getSortableId() {
        try {
            return new URI("urn:" + this.type + ":" + this.id);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public URI getSortableParentId() {
        try {
            return new URI("urn:" + this.parentType + ":" + this.parentId);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public Optional<SortableResourceConnection> getResourceConnection() {
        return Optional.ofNullable(resourceConnection);
    }
}
