/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Resource;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.domain.TopicResource;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TopicResourceTreeSortableTest {
    @Test
    public void getSortableRank() {

        // TopicResource
        var sortable = new TopicResourceTreeSortable("resource", "topic", 100, 10, 1000);
        assertEquals(1000 - 10000, sortable.getSortableRank());

        // TopicSubtopic
        sortable = new TopicResourceTreeSortable("topic", "topic", 100, 10, 800);
        assertEquals(800 - 1000, sortable.getSortableRank());
    }

    @Test
    public void getSortableId() throws URISyntaxException {
        final var sortable = new TopicResourceTreeSortable("resource", "topic", 100, 10, 1000);
        assertEquals(new URI("urn:resource:100"), sortable.getSortableId());
    }

    @Test
    public void getSortableParentId() throws URISyntaxException {
        final var sortable = new TopicResourceTreeSortable("resource", "topic", 100, 10, 1000);
        assertEquals(new URI("urn:topic:10"), sortable.getSortableParentId());
    }

    @Test
    public void getTopicResource() {
        var sortable = new TopicResourceTreeSortable("resource", "topic", 100, 10, 1000);
        assertFalse(sortable.getTopicResource().isPresent());

        final var topic = mock(Topic.class);
        final var resource = mock(Resource.class);
        final var topicResource = mock(TopicResource.class);

        when(topic.getId()).thenReturn(101);
        when(resource.getId()).thenReturn(102);

        when(topicResource.getTopic()).thenReturn(Optional.of(topic));
        when(topicResource.getResource()).thenReturn(Optional.of(resource));

        when(topicResource.getRank()).thenReturn(200 - 10000);

        sortable = new TopicResourceTreeSortable(topicResource);
        assertSame(topicResource, sortable.getTopicResource().orElse(null));
    }

    @Test
    public void testPopulateFromTopicResource() throws URISyntaxException {
        final var topic = mock(Topic.class);
        final var resource = mock(Resource.class);
        final var topicResource = mock(TopicResource.class);

        when(topic.getId()).thenReturn(101);
        when(resource.getId()).thenReturn(102);

        when(topicResource.getTopic()).thenReturn(Optional.of(topic));
        when(topicResource.getResource()).thenReturn(Optional.of(resource));

        when(topicResource.getRank()).thenReturn(200);

        final var sortable = new TopicResourceTreeSortable(topicResource);

        assertEquals(200 - 10000, sortable.getSortableRank());
        assertEquals(new URI("urn:topic:101"), sortable.getSortableParentId());
        assertEquals(new URI("urn:resource:102"), sortable.getSortableId());
    }
}