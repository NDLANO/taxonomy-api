package no.ndla.taxonomy.repositories;


import no.ndla.taxonomy.domain.ResourceType;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.domain.TopicResourceType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import javax.transaction.Transactional;
import java.net.URI;
import java.util.Iterator;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("junit")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
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
        assertEquals(getClass().getCanonicalName()+" test topic", topicResourceType.getTopic().getName());
        assertEquals(getClass().getCanonicalName() + " test resource type", topicResourceType.getResourceType().getName());
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
                Iterator<TopicResourceType> topicResourceTypes = topic.topicResourceTypes.iterator();
                assertTrue(topicResourceTypes.hasNext());
                topicResourceType = topicResourceTypes.next();
                assertFalse(topicResourceTypes.hasNext());
            }
            assertEquals(testTopicResourceType, topicResourceType.getPublicId());
            resourceType = topicResourceType.getResourceType();
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
        topic = topicRepository.save(topic);
        assertFalse(topic.topicResourceTypes.iterator().hasNext());
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
        assertEquals(testResourceType, topicResourceType.getResourceType().getPublicId());
    }

    @After
    public void afterTesting() {
        try {
            topicRepository.deleteByPublicId(testTopic);
        } finally {
            resourceTypeRepository.deleteByPublicId(testResourceType);
        }
        assertNotNull(testTopicResourceType);
        assertNull(topicResourceTypeRepository.findByPublicId(testTopicResourceType));
    }

    @Before
    public void beforeTesting() {
        this.testTopic = null;
        this.testResourceType = null;
        this.testTopicResourceType = null;
        Topic testTopic = topicRepository.save(new Topic().name(getClass().getCanonicalName()+" test topic"));
        ResourceType testResourceType = null;
        TopicResourceType testTopicResourceType = null;
        try {
            testResourceType = resourceTypeRepository.save(new ResourceType().name(getClass().getCanonicalName() + " test resource type"));
            testTopicResourceType = testTopic.addResourceType(testResourceType);
            testTopic = topicRepository.save(testTopic);
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
