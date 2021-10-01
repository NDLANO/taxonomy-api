/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.TopicResource;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

public class TopicResourceTreeSortable implements TopicTreeSorter.Sortable {
    private int rank;
    private int id;
    private int parentId;
    private TopicResource topicResource;
    private String parentType;
    private String type;

    public TopicResourceTreeSortable(TopicResource topicResource) {
        this.id = topicResource.getResource().orElseThrow(() -> new IllegalArgumentException("Resource not set")).getId();
        this.parentId = topicResource.getTopic().orElseThrow(() -> new IllegalArgumentException("Topic not set")).getId();
        this.rank = topicResource.getRank();
        this.topicResource = topicResource;
        this.type = "resource";
        this.parentType = "topic";
    }

    public TopicResourceTreeSortable(String type, String parentType, int id, int parentId, int rank) {
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

    public Optional<TopicResource> getTopicResource() {
        return Optional.ofNullable(topicResource);
    }
}
