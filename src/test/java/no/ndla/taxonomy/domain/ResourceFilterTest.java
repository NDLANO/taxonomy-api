package no.ndla.taxonomy.domain;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ResourceFilterTest {
    private Relevance relevance;
    private Filter filter;
    private Resource resource;
    private ResourceFilter resourceFilter;

    @Before
    public void setUp() {
        relevance = mock(Relevance.class);
        filter = mock(Filter.class);
        resource = mock(Resource.class);

        resourceFilter = ResourceFilter.create(resource, filter, relevance);

        verify(resource).addResourceFilter(resourceFilter);
        verify(filter).addResourceFilter(resourceFilter);
        verify(relevance).addResourceFilter(resourceFilter);
    }

    @Test
    public void getFilter() {
        assertSame(filter, resourceFilter.getFilter());
    }

    @Test
    public void getResource() {
        assertSame(resource, resourceFilter.getResource());
    }

    @Test
    public void getRelevance() {
        assertSame(relevance, resourceFilter.getRelevance().orElse(null));
    }
}