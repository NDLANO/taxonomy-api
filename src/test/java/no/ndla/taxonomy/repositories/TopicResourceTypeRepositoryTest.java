/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.repositories;


import no.ndla.taxonomy.domain.ResourceType;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.domain.TopicResourceType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.transaction.Transactional;
import java.net.URI;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("junit")
@Transactional
public class TopicResourceTypeRepositoryTest {
    private URI testTopic;
    private URI testResourceType;
    private URI testTopicResourceType;

    @Autowired
    private TopicRepository topicRepository;
    @Autowired
    private ResourceTypeRepository resourceTypeRepository;
    @Autowired
    private TopicResourceTypeRepository topicResourceTypeRepository;

    @Test
    public void testLoadTopicResourceType() {
        assertNotNull(testTopicResourceType);
        TopicResourceType topicResourceType = topicResourceTypeRepository.findByPublicId(testTopicResourceType);
        assertNotNull(testTopicResourceType);
        assertEquals(getClass().getCanonicalName() + " test topic", topicResourceType.getTopic().map(Topic::getName).orElse(""));
        assertEquals(getClass().getCanonicalName() + " test resource type", topicResourceType.getResourceType().map(ResourceType::getName).orElse(""));
    }

    @Test
    public void testHasTopicResourceType() {
        assertNotNull(testTopic);
        Topic topic = topicRepository.findByPublicId(testTopic);
        assertNotNull(topic);
        ResourceType resourceType;
        {
            TopicResourceType topicResourceType;
            {
                Iterator<TopicResourceType> topicResourceTypes = topic.getTopicResourceTypes().iterator();
                assertTrue(topicResourceTypes.hasNext());
                topicResourceType = topicResourceTypes.next();
                assertFalse(topicResourceTypes.hasNext());
            }
            assertEquals(testTopicResourceType, topicResourceType.getPublicId());
            resourceType = topicResourceType.getResourceType().orElse(null);
        }
        assertEquals(testResourceType, resourceType.getPublicId());
    }

    @Test
    public void testRemoveResourceType() {
        assertNotNull(testTopic);
        assertNotNull(testResourceType);
        Topic topic = topicRepository.findByPublicId(testTopic);
        assertNotNull(topic);
        ResourceType resourceType = resourceTypeRepository.findByPublicId(testResourceType);
        assertNotNull(resourceType);
        topic.removeResourceType(resourceType);
        topic = topicRepository.saveAndFlush(topic);
        assertFalse(topic.getTopicResourceTypes().iterator().hasNext());
    }

    @Test
    public void testQueryForTopicResourceTypes() {
        assertNotNull(testTopic);
        Topic topic = topicRepository.findByPublicId(testTopic);
        assertNotNull(topic);
        Iterator<TopicResourceType> topicResourceTypes = topicResourceTypeRepository.findAllByTopic(topic).iterator();
        assertTrue(topicResourceTypes.hasNext());
        TopicResourceType topicResourceType = topicResourceTypes.next();
        assertFalse(topicResourceTypes.hasNext());
        assertEquals(testTopicResourceType, topicResourceType.getPublicId());
        assertEquals(testResourceType, topicResourceType.getResourceType().map(ResourceType::getPublicId).orElse(null));
    }

    @AfterEach
    public void afterTesting() {
        try {
            topicRepository.deleteByPublicId(testTopic);
        } finally {
            resourceTypeRepository.deleteByPublicId(testResourceType);
        }
        assertNotNull(testTopicResourceType);
        assertNull(topicResourceTypeRepository.findByPublicId(testTopicResourceType));
    }

    @BeforeEach
    public void beforeTesting() {
        this.testTopic = null;
        this.testResourceType = null;
        this.testTopicResourceType = null;
        Topic testTopic = topicRepository.saveAndFlush(new Topic().name(getClass().getCanonicalName() + " test topic"));
        ResourceType testResourceType = null;
        TopicResourceType testTopicResourceType = null;
        try {
            testResourceType = resourceTypeRepository.saveAndFlush(new ResourceType().name(getClass().getCanonicalName() + " test resource type"));
            testTopicResourceType = testTopic.addResourceType(testResourceType);
            testTopic = topicRepository.saveAndFlush(testTopic);
            this.testTopic = testTopic.getPublicId();
            this.testResourceType = testResourceType.getPublicId();
            this.testTopicResourceType = testTopicResourceType.getPublicId();
            assertNotNull(testTopicResourceType);
        } finally {
            if (this.testTopicResourceType == null) {
                try {
                    if (testTopic != null) {
                        topicRepository.delete(testTopic);
                    }
                } finally {
                    if (testResourceType != null) {
                        resourceTypeRepository.delete(testResourceType);
                    }
                }
            }
        }
    }
}
