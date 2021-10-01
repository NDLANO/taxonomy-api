/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class TopicResourceTypeTest {
    private Topic topic;
    private ResourceType resourceType;
    private TopicResourceType topicResourceType;

    @BeforeEach
    public void setUp() {
        topic = mock(Topic.class);
        resourceType = mock(ResourceType.class);

        topicResourceType = TopicResourceType.create(topic, resourceType);
        assertNotNull(topicResourceType.getPublicId());
        verify(topic).addTopicResourceType(topicResourceType);
        verify(resourceType).addTopicResourceType(topicResourceType);
    }

    @Test
    public void getTopic() {
        assertSame(topic, topicResourceType.getTopic().orElse(null));
    }

    @Test
    public void getResourceType() {
        assertSame(resourceType, topicResourceType.getResourceType().orElse(null));
    }
}