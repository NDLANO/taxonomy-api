/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class TopicResourceTest {
    private Topic topic;
    private Resource resource;
    private TopicResource topicResource;

    @BeforeEach
    public void setUp() {
        topic = mock(Topic.class);
        resource = mock(Resource.class);

        topicResource = TopicResource.create(topic, resource);

        verify(topic).addTopicResource(topicResource);
        verify(resource).addTopicResource(topicResource);

        assertNotNull(topicResource.getPublicId());
        assertTrue(topicResource.getPublicId().toString().length() > 4);
    }

    @Test
    public void getTopic() {
        assertSame(topic, topicResource.getTopic().orElse(null));
    }

    @Test
    public void getResource() {
        assertSame(resource, topicResource.getResource().orElse(null));
    }

    @Test
    public void setAndIsPrimary() {
        assertFalse(topicResource.isPrimary().orElseThrow());
        topicResource.setPrimary(true);
        assertTrue(topicResource.isPrimary().orElseThrow());
        topicResource.setPrimary(false);
        assertFalse(topicResource.isPrimary().orElseThrow());
    }

    @Test
    public void setAndGetRank() {
        assertEquals(0, topicResource.getRank());
        topicResource.setRank(100);
        assertEquals(100, topicResource.getRank());
    }

    @Test
    public void preRemove() {
        topicResource.preRemove();

        assertFalse(topicResource.getResource().isPresent());
        assertFalse(topicResource.getTopic().isPresent());

        verify(topic).removeTopicResource(topicResource);
        verify(resource).removeTopicResource(topicResource);
    }
}
