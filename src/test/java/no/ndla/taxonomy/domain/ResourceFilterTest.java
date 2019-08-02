package no.ndla.taxonomy.domain;

import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ResourceFilterTest {
    private ResourceFilter resourceFilter;

    @Before
    public void setUp() {
        resourceFilter = new ResourceFilter();
    }

    @Test
    public void testConstructor() {
        final var relevance = mock(Relevance.class);
        final var filter = mock(Filter.class);
        final var resource = mock(Resource.class);

        var createdResourceFilter = new ResourceFilter(resource, filter, relevance);

        assertEquals(resource, createdResourceFilter.getResource());
        assertEquals(filter, createdResourceFilter.getFilter());
        assertTrue(createdResourceFilter.getRelevance().isPresent());
        assertEquals(relevance, createdResourceFilter.getRelevance().get());

        verify(relevance).addResourceFilter(createdResourceFilter);
        verify(filter).addResourceFilter(createdResourceFilter);
        verify(resource).addResourceFilter(createdResourceFilter);

        assertNotNull(createdResourceFilter.getPublicId());
        assertTrue(createdResourceFilter.getPublicId().toString().length() > 4);
    }

    @Test
    public void setAndGetFilter() {
        final var filter1 = mock(Filter.class);
        final var filter2 = mock(Filter.class);

        assertNull(resourceFilter.getFilter());

        resourceFilter.setFilter(filter1);
        verify(filter1).addResourceFilter(resourceFilter);

        assertEquals(filter1, resourceFilter.getFilter());

        resourceFilter.setFilter(filter2);

        verify(filter2).addResourceFilter(resourceFilter);
        verify(filter1).removeResourceFilter(resourceFilter);

        assertEquals(filter2, resourceFilter.getFilter());
    }

    @Test
    public void setAndGetResource() {
        final var resource1 = mock(Resource.class);
        final var resource2 = mock(Resource.class);

        assertNull(resourceFilter.getResource());

        resourceFilter.setResource(resource1);
        verify(resource1).addResourceFilter(resourceFilter);

        assertEquals(resource1, resourceFilter.getResource());

        resourceFilter.setResource(resource2);

        verify(resource2).addResourceFilter(resourceFilter);
        verify(resource1).removeResourceFilter(resourceFilter);

        assertEquals(resource2, resourceFilter.getResource());
    }

    @Test
    public void setAndGetRelevance() {
        final var relevance1 = mock(Relevance.class);
        final var relevance2 = mock(Relevance.class);

        assertFalse(resourceFilter.getRelevance().isPresent());

        resourceFilter.setRelevance(relevance1);
        verify(relevance1).addResourceFilter(resourceFilter);

        assertTrue(resourceFilter.getRelevance().isPresent());
        assertEquals(relevance1, resourceFilter.getRelevance().get());

        resourceFilter.setRelevance(relevance2);

        verify(relevance2).addResourceFilter(resourceFilter);
        verify(relevance1).removeResourceFilter(resourceFilter);

        assertTrue(resourceFilter.getRelevance().isPresent());
        assertEquals(relevance2, resourceFilter.getRelevance().get());
    }

    @Test
    public void testToString() throws URISyntaxException {
        final var relevance = mock(Relevance.class);
        final var filter = mock(Filter.class);
        final var resource = mock(Resource.class);

        resourceFilter.setRelevance(relevance);
        resourceFilter.setFilter(filter);
        resourceFilter.setResource(resource);

        when(relevance.getName()).thenReturn("Relevance Name");
        when(filter.getName()).thenReturn("Filter Name");
        when(resource.getName()).thenReturn("Resource Name");

        when(filter.getPublicId()).thenReturn(new URI("urn:filter"));
        when(resource.getPublicId()).thenReturn(new URI("urn:resource"));

        assertEquals("ResourceFilter: { Resource Name urn:resource --Relevance Name--> Filter Name urn:filter }", resourceFilter.toString());
    }
}