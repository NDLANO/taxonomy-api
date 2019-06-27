package no.ndla.taxonomy.domain;

import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

public class ResourceTest {
    private Resource resource;

    @Before
    public void setUp() {
        this.resource = new Resource();
    }

    @Test
    public void addAndGetAndRemoveTranslations() {
        assertEquals(0, resource.getTranslations().size());

        var returnedTranslation = resource.addTranslation("nb");
        assertEquals(1, resource.getTranslations().size());
        assertEquals("nb", returnedTranslation.getLanguageCode());
        assertTrue(resource.getTranslations().contains(returnedTranslation));
        assertEquals(resource, returnedTranslation.getResource());

        var returnedTranslation2 = resource.addTranslation("en");
        assertEquals(2, resource.getTranslations().size());
        assertEquals("en", returnedTranslation2.getLanguageCode());
        assertTrue(resource.getTranslations().containsAll(Set.of(returnedTranslation, returnedTranslation2)));
        assertEquals(resource, returnedTranslation2.getResource());

        resource.removeTranslation("nb");

        assertNull(returnedTranslation.getResource());
        assertFalse(resource.getTranslations().contains(returnedTranslation));

        assertFalse(resource.getTranslation("nb").isPresent());

        resource.addTranslation(returnedTranslation);
        assertEquals(resource, returnedTranslation.getResource());
        assertTrue(resource.getTranslations().contains(returnedTranslation));

        assertEquals(returnedTranslation, resource.getTranslation("nb").get());
        assertEquals(returnedTranslation2, resource.getTranslation("en").get());
    }


    @Test
    public void getAndSetName() {
        assertNull(resource.getName());
        resource.setName("test name");
        assertEquals("test name", resource.getName());
    }

    @Test
    public void getTopics() {
        final var topic1 = mock(Topic.class);
        final var topic2 = mock(Topic.class);

        assertEquals(0, resource.getTopics().size());

        final var topicResource1 = mock(TopicResource.class);
        final var topicResource2 = mock(TopicResource.class);

        when(topicResource1.getTopic()).thenReturn(topic1);
        when(topicResource2.getTopic()).thenReturn(topic2);

        setField(resource, "topics", Set.of(topicResource1, topicResource2));

        assertEquals(2, resource.getTopics().size());
        assertTrue(resource.getTopics().containsAll(Set.of(topic1, topic2)));
    }

    @Test
    public void getAddAndRemoveResourceTypes() {
        final var resourceType1 = mock(ResourceType.class);
        final var resourceType2 = mock(ResourceType.class);

        when(resourceType1.getId()).thenReturn(100);
        when(resourceType2.getId()).thenReturn(101);

        assertEquals(0, resource.getResourceTypes().size());

        resource.addResourceType(resourceType1);
        assertEquals(1, resource.getResourceTypes().size());
        assertTrue(resource.getResourceTypes().contains(resourceType1));
        resource.addResourceType(resourceType2);
        assertEquals(2, resource.getResourceTypes().size());
        assertTrue(resource.getResourceTypes().containsAll(Set.of(resourceType1, resourceType2)));

        try {
            resource.addResourceType(resourceType2);
            fail("Expected DuplicateIdException");
        } catch (DuplicateIdException ignored) {

        }

        resource.removeResourceType(resourceType1);
        assertEquals(1, resource.getResourceTypes().size());
        assertTrue(resource.getResourceTypes().contains(resourceType2));
        resource.removeResourceType(resourceType2);
        assertEquals(0, resource.getResourceTypes().size());
    }
/*
    @Test
    public void addRemoveAndGetResourceResourceType() {
    }

    @Test
    public void removeResourceResourceType() {
    }

    @Test
    public void getContentUri() {
    }

    @Test
    public void setContentUri() {
    }

    @Test
    public void getPrimaryTopic() {
    }

    @Test
    public void setPrimaryTopic() {
    }

    @Test
    public void setRandomPrimaryTopic() {
    }

    @Test
    public void hasSingleParentTopic() {
    }

    @Test
    public void removeResourceType() {
    }

    @Test
    public void addFilter() {
    }

    @Test
    public void removeFilter() {
    }

    @Test
    public void preRemove() {
    }

    @Test
    public void getResourceResourceTypes() {
    }

    @Test
    public void getResourceFilters() {
    }

    @Test
    public void getTopicResources() {
    }

    @Test
    public void removeTopicResource() {
    }

    @Test
    public void addResourceFilter() {
    }

    @Test
    public void removeResourceFilter() {
    } */
}