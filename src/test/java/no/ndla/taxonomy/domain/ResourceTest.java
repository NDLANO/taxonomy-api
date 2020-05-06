package no.ndla.taxonomy.domain;

import no.ndla.taxonomy.domain.exceptions.DuplicateIdException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

public class ResourceTest {
    private Resource resource;

    @BeforeEach
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

        try {
            resource.addResourceResourceType(resourceResourceType1);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {

        }

        when(resourceResourceType1.getResource()).thenReturn(resource);
        resource.addResourceResourceType(resourceResourceType1);

        assertEquals(1, resource.getResourceResourceTypes().size());
        assertTrue(resource.getResourceResourceTypes().contains(resourceResourceType1));

        try {
            resource.addResourceResourceType(resourceResourceType2);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }

        when(resourceResourceType2.getResource()).thenReturn(resource);
        resource.addResourceResourceType(resourceResourceType2);

        assertEquals(2, resource.getResourceResourceTypes().size());
        assertTrue(resource
                .getResourceResourceTypes()
                .containsAll(Set.of(resourceResourceType1, resourceResourceType2))
        );

        reset(resourceResourceType1);
        reset(resourceResourceType2);

        when(resourceResourceType1.getResource()).thenReturn(resource);
        when(resourceResourceType2.getResource()).thenReturn(resource);

        resource.removeResourceResourceType(resourceResourceType1);
        assertEquals(1, resource.getResourceResourceTypes().size());
        assertTrue(resource.getResourceResourceTypes().contains(resourceResourceType2));
        verify(resourceResourceType1).disassociate();

        resource.removeResourceResourceType(resourceResourceType2);
        assertEquals(0, resource.getResourceResourceTypes().size());
        verify(resourceResourceType2).disassociate();
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

        try {
            resource.addResourceFilter(resourceFilter1);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }

        when(resourceFilter1.getResource()).thenReturn(resource);
        resource.addResourceFilter(resourceFilter1);

        assertEquals(1, resource.getResourceFilters().size());
        assertTrue(resource.getResourceFilters().contains(resourceFilter1));

        try {
            resource.addResourceFilter(resourceFilter2);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {

        }
        when(resourceFilter2.getResource()).thenReturn(resource);
        resource.addResourceFilter(resourceFilter2);

        assertEquals(2, resource.getResourceFilters().size());
        assertTrue(resource.getResourceFilters().containsAll(Set.of(resourceFilter1, resourceFilter2)));

        when(resourceFilter1.getResource()).thenReturn(resource);
        when(resourceFilter2.getResource()).thenReturn(resource);

        resource.removeResourceFilter(resourceFilter1);

        verify(resourceFilter1).disassociate();

        assertEquals(1, resource.getResourceFilters().size());
        assertTrue(resource.getResourceFilters().contains(resourceFilter2));

        resource.removeResourceFilter(resourceFilter2);

        verify(resourceFilter2).disassociate();
        assertEquals(0, resource.getResourceFilters().size());
    }

    @Test
    public void addGetAndRemoveTopicResources() {
        final var topicResource1 = mock(TopicResource.class);
        final var topicResource2 = mock(TopicResource.class);

        assertEquals(0, resource.getTopicResources().size());

        try {
            resource.addTopicResource(topicResource1);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }

        when(topicResource1.getResource()).thenReturn(Optional.of(resource));
        resource.addTopicResource(topicResource1);

        assertEquals(1, resource.getTopicResources().size());
        assertTrue(resource.getTopicResources().contains(topicResource1));

        try {
            resource.addTopicResource(topicResource2);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }

        when(topicResource2.getResource()).thenReturn(Optional.of(resource));
        resource.addTopicResource(topicResource2);

        assertEquals(2, resource.getTopicResources().size());
        assertTrue(resource.getTopicResources().containsAll(Set.of(topicResource1, topicResource2)));

        when(topicResource1.getResource()).thenReturn(Optional.of(resource));
        when(topicResource2.getResource()).thenReturn(Optional.of(resource));

        resource.removeTopicResource(topicResource1);

        verify(topicResource1).disassociate();

        assertEquals(1, resource.getTopicResources().size());
        assertTrue(resource.getTopicResources().contains(topicResource2));

        resource.removeTopicResource(topicResource2);

        verify(topicResource2).disassociate();
        assertEquals(0, resource.getTopicResources().size());
    }

    @Test
    public void preRemove() {
        final var topicResource1 = mock(TopicResource.class);
        final var topicResource2 = mock(TopicResource.class);
        final var resourceFilter1 = mock(ResourceFilter.class);
        final var resourceFilter2 = mock(ResourceFilter.class);


        Set.of(topicResource1, topicResource2).forEach(topicResource -> {
            when(topicResource.getResource()).thenReturn(Optional.of(resource));
            resource.addTopicResource(topicResource);
        });

        Set.of(resourceFilter1, resourceFilter2).forEach(resourceFilter -> {
            when(resourceFilter.getResource()).thenReturn(resource);
            resource.addResourceFilter(resourceFilter);
        });

        resource.preRemove();

        verify(topicResource1).disassociate();
        verify(topicResource2).disassociate();
        verify(resourceFilter1).disassociate();
        verify(resourceFilter2).disassociate();
    }

    @Test
    public void isContext() {
        final var resource = new Resource();
        assertFalse(resource.isContext());
    }

    @Test
    public void getCachedPaths() {
        final var cachedPaths = Set.of();

        setField(resource, "cachedPaths", cachedPaths);
        assertSame(cachedPaths, resource.getCachedPaths());
    }
}