package no.ndla.taxonomy.domain;

import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
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

        when(topicResource1.getTopic()).thenReturn(Optional.of(topic1));
        when(topicResource2.getTopic()).thenReturn(Optional.of(topic2));

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

    @Test
    public void addRemoveAndGetResourceResourceType() {
        final var resourceResourceType1 = mock(ResourceResourceType.class);
        final var resourceResourceType2 = mock(ResourceResourceType.class);

        assertEquals(0, resource.getResourceResourceTypes().size());

        resource.addResourceResourceType(resourceResourceType1);
        assertEquals(1, resource.getResourceResourceTypes().size());
        assertTrue(resource.getResourceResourceTypes().contains(resourceResourceType1));
        verify(resourceResourceType1).setResource(resource);

        resource.addResourceResourceType(resourceResourceType2);
        assertEquals(2, resource.getResourceResourceTypes().size());
        assertTrue(resource
                .getResourceResourceTypes()
                .containsAll(Set.of(resourceResourceType1, resourceResourceType2))
        );
        verify(resourceResourceType2).setResource(resource);

        reset(resourceResourceType1);
        reset(resourceResourceType2);

        when(resourceResourceType1.getResource()).thenReturn(resource);
        when(resourceResourceType2.getResource()).thenReturn(resource);

        resource.removeResourceResourceType(resourceResourceType1);
        assertEquals(1, resource.getResourceResourceTypes().size());
        assertTrue(resource.getResourceResourceTypes().contains(resourceResourceType2));
        verify(resourceResourceType1).setResource(null);

        resource.removeResourceResourceType(resourceResourceType2);
        assertEquals(0, resource.getResourceResourceTypes().size());
        verify(resourceResourceType2).setResource(null);
    }

    @Test
    public void setAndGetContentUri() throws URISyntaxException {
        assertNull(resource.getContentUri());

        final var uri1 = new URI("urn:test1");
        final var uri2 = new URI("urn:test2");

        resource.setContentUri(uri1);
        assertEquals("urn:test1", resource.getContentUri().toString());

        resource.setContentUri(uri2);
        assertEquals("urn:test2", resource.getContentUri().toString());
    }

    @Test
    public void setAndGetPrimaryTopic() {
        // Resource is not assigned to any topics
        assertFalse(resource.getPrimaryTopic().isPresent());

        final var topic1 = mock(Topic.class);
        final var topic2 = mock(Topic.class);
        final var topic3 = mock(Topic.class);

        final var topicResource1 = mock(TopicResource.class);
        final var topicResource2 = mock(TopicResource.class);
        final var topicResource3 = mock(TopicResource.class);

        when(topicResource1.getTopic()).thenReturn(Optional.of(topic1));
        when(topicResource2.getTopic()).thenReturn(Optional.of(topic2));
        when(topicResource3.getTopic()).thenReturn(Optional.of(topic3));

        Set.of(topicResource1, topicResource2, topicResource3).forEach(topicResource -> {
            when(topicResource.getResource()).thenReturn(Optional.of(resource));
            resource.addTopicResource(topicResource);
        });

        when(topicResource1.isPrimary()).thenReturn(false);
        when(topicResource2.isPrimary()).thenReturn(false);
        when(topicResource3.isPrimary()).thenReturn(false);

        // None of the TopicResource objects is set as primary
        // It is possible, but it is not supposed to happen outside test
        assertFalse(resource.getPrimaryTopic().isPresent());

        when(topicResource2.isPrimary()).thenReturn(true);
        assertEquals(topic2, resource.getPrimaryTopic().orElse(null));

        when(topicResource1.isPrimary()).thenReturn(true);
        when(topicResource2.isPrimary()).thenReturn(false);
        assertEquals(topic1, resource.getPrimaryTopic().orElse(null));

        // Replicates the setPrimary method as it does in the real objects
        Set.of(topicResource1, topicResource2, topicResource3).forEach(topicResource -> doAnswer(invocationOnMock -> {
            final var setPrimary = (boolean) invocationOnMock.getArgument(0);
            when(topicResource.isPrimary()).thenReturn(setPrimary);

            return null;
        }).when(topicResource).setPrimary(anyBoolean()));

        resource.setPrimaryTopic(topic2);
        assertEquals(topic2, resource.getPrimaryTopic().orElse(null));

        resource.setPrimaryTopic(topic3);
        assertEquals(topic3, resource.getPrimaryTopic().orElse(null));
    }

    @Test
    public void setRandomPrimaryTopic() {
        final var topic1 = mock(Topic.class);
        final var topic2 = mock(Topic.class);
        final var topic3 = mock(Topic.class);

        final var topicResource1 = mock(TopicResource.class);
        final var topicResource2 = mock(TopicResource.class);
        final var topicResource3 = mock(TopicResource.class);

        when(topicResource1.getTopic()).thenReturn(Optional.of(topic1));
        when(topicResource2.getTopic()).thenReturn(Optional.of(topic2));
        when(topicResource3.getTopic()).thenReturn(Optional.of(topic3));

        Set.of(topicResource1, topicResource2, topicResource3).forEach(topicResource -> {
            when(topicResource.getResource()).thenReturn(Optional.of(resource));
            resource.addTopicResource(topicResource);
        });

        when(topicResource1.isPrimary()).thenReturn(false);
        when(topicResource2.isPrimary()).thenReturn(false);
        when(topicResource3.isPrimary()).thenReturn(false);

        // Replicates the setPrimary method as it does in the real objects
        Set.of(topicResource1, topicResource2, topicResource3).forEach(topicResource -> doAnswer(invocationOnMock -> {
            final var setPrimary = (boolean) invocationOnMock.getArgument(0);
            when(topicResource.isPrimary()).thenReturn(setPrimary);

            return null;
        }).when(topicResource).setPrimary(anyBoolean()));

        assertFalse(resource.getPrimaryTopic().isPresent());

        resource.setRandomPrimaryTopic();

        // Any of the three topics could become primary
        assertTrue(resource.getPrimaryTopic().isPresent());
        assertTrue(Set.of(topic1, topic2, topic3).contains(resource.getPrimaryTopic().get()));
    }

    @Test
    public void addAndRemoveResourceType() {
        assertEquals(0, resource.getResourceTypes().size());

        final var resourceType1 = mock(ResourceType.class);
        resource.addResourceType(resourceType1);

        assertEquals(1, resource.getResourceTypes().size());
        assertTrue(resource.getResourceTypes().contains(resourceType1));

        final var resourceType2 = mock(ResourceType.class);
        resource.addResourceType(resourceType2);

        assertEquals(2, resource.getResourceTypes().size());
        assertTrue(resource.getResourceTypes().containsAll(Set.of(resourceType1, resourceType2)));

        resource.removeResourceType(resourceType1);
        assertEquals(1, resource.getResourceTypes().size());
        assertTrue(resource.getResourceTypes().contains(resourceType2));

        resource.removeResourceType(resourceType2);
        assertEquals(0, resource.getResourceTypes().size());
    }

    @Test
    public void addAndRemoveFilter() {
        final var filter1 = mock(Filter.class);
        final var filter2 = mock(Filter.class);

        final var relevance1 = mock(Relevance.class);
        final var relevance2 = mock(Relevance.class);

        assertEquals(0, resource.getResourceFilters().size());

        doAnswer(invocationOnMock -> {
            assertEquals(filter1, ((ResourceFilter) invocationOnMock.getArgument(0)).getFilter());

            return null;
        }).when(filter1).addResourceFilter(any(ResourceFilter.class));

        doAnswer(invocationOnMock -> {
            assertEquals(filter1, ((ResourceFilter) invocationOnMock.getArgument(0)).getFilter());

            return null;
        }).when(relevance1).addResourceFilter(any(ResourceFilter.class));

        resource.addFilter(filter1, relevance1);

        assertEquals(1, resource.getResourceFilters().size());
        assertTrue(resource.getResourceFilters().stream().map(ResourceFilter::getFilter).collect(Collectors.toSet()).contains(filter1));

        resource.addFilter(filter2, relevance2);

        assertEquals(2, resource.getResourceFilters().size());
        assertTrue(resource.getResourceFilters().stream().map(ResourceFilter::getFilter).collect(Collectors.toSet()).containsAll(Set.of(filter1, filter2)));

        verify(filter1, times(1)).addResourceFilter(any(ResourceFilter.class));
        verify(relevance1, times(1)).addResourceFilter(any(ResourceFilter.class));

        verify(filter2, times(1)).addResourceFilter(any(ResourceFilter.class));
        verify(relevance2, times(1)).addResourceFilter(any(ResourceFilter.class));

        final var resourceFilter1 = resource
                .getResourceFilters()
                .stream()
                .filter(resourceFilter -> resourceFilter.getFilter().equals(filter1))
                .findFirst()
                .orElse(null);

        final var resourceFilter2 = resource
                .getResourceFilters()
                .stream()
                .filter(resourceFilter -> resourceFilter.getFilter().equals(filter2))
                .findFirst()
                .orElse(null);

        resource.removeFilter(filter1);

        assertEquals(1, resource.getResourceFilters().size());
        assertTrue(resource.getResourceFilters().stream().map(ResourceFilter::getFilter).collect(Collectors.toSet()).contains(filter2));

        verify(filter1).removeResourceFilter(resourceFilter1);
        verify(relevance1).removeResourceFilter(resourceFilter1);

        resource.removeFilter(filter2);

        assertEquals(0, resource.getResourceFilters().size());
        verify(filter2).removeResourceFilter(resourceFilter2);
        verify(relevance2).removeResourceFilter(resourceFilter2);
    }

    @Test
    public void addGetAndRemoveResourceFilters() {
        final var resourceFilter1 = mock(ResourceFilter.class);
        final var resourceFilter2 = mock(ResourceFilter.class);

        assertEquals(0, resource.getResourceFilters().size());

        resource.addResourceFilter(resourceFilter1);

        assertEquals(1, resource.getResourceFilters().size());
        assertTrue(resource.getResourceFilters().contains(resourceFilter1));

        verify(resourceFilter1).setResource(resource);

        resource.addResourceFilter(resourceFilter2);

        assertEquals(2, resource.getResourceFilters().size());
        assertTrue(resource.getResourceFilters().containsAll(Set.of(resourceFilter1, resourceFilter2)));

        verify(resourceFilter2).setResource(resource);

        when(resourceFilter1.getResource()).thenReturn(resource);
        when(resourceFilter2.getResource()).thenReturn(resource);

        resource.removeResourceFilter(resourceFilter1);

        verify(resourceFilter1).setResource(null);

        assertEquals(1, resource.getResourceFilters().size());
        assertTrue(resource.getResourceFilters().contains(resourceFilter2));

        resource.removeResourceFilter(resourceFilter2);

        verify(resourceFilter2).setResource(null);
        assertEquals(0, resource.getResourceFilters().size());
    }

    @Test
    public void addGetAndRemoveTopicResources() {
        final var topicResource1 = mock(TopicResource.class);
        final var topicResource2 = mock(TopicResource.class);

        assertEquals(0, resource.getTopicResources().size());

        resource.addTopicResource(topicResource1);

        assertEquals(1, resource.getTopicResources().size());
        assertTrue(resource.getTopicResources().contains(topicResource1));

        verify(topicResource1).setResource(resource);

        resource.addTopicResource(topicResource2);

        assertEquals(2, resource.getTopicResources().size());
        assertTrue(resource.getTopicResources().containsAll(Set.of(topicResource1, topicResource2)));

        verify(topicResource2).setResource(resource);

        when(topicResource1.getResource()).thenReturn(Optional.of(resource));
        when(topicResource2.getResource()).thenReturn(Optional.of(resource));

        resource.removeTopicResource(topicResource1);

        verify(topicResource1).setResource(null);

        assertEquals(1, resource.getTopicResources().size());
        assertTrue(resource.getTopicResources().contains(topicResource2));

        resource.removeTopicResource(topicResource2);

        verify(topicResource2).setResource(null);
        assertEquals(0, resource.getTopicResources().size());
    }

    @Test
    public void preRemove() {
        final var resourceSpy = spy(resource);

        final var topicResource1 = mock(TopicResource.class);
        final var topicResource2 = mock(TopicResource.class);
        final var resourceFilter1 = mock(ResourceFilter.class);
        final var resourceFilter2 = mock(ResourceFilter.class);

        Set.of(topicResource1, topicResource2).forEach(resourceSpy::addTopicResource);
        Set.of(resourceFilter1, resourceFilter2).forEach(resourceSpy::addResourceFilter);

        doAnswer(invocationOnMock -> null).when(resourceSpy).removeTopicResource(any(TopicResource.class));
        doAnswer(invocationOnMock -> null).when(resourceSpy).removeResourceFilter(any(ResourceFilter.class));

        resourceSpy.preRemove();

        Set.of(topicResource1, topicResource2).forEach(topicResource -> verify(resourceSpy).removeTopicResource(topicResource));
        Set.of(resourceFilter1, resourceFilter2).forEach(resourceFilter -> verify(resourceSpy).removeResourceFilter(resourceFilter));
    }
}