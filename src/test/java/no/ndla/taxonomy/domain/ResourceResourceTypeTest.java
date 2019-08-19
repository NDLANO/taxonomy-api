package no.ndla.taxonomy.domain;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ResourceResourceTypeTest {
    private Resource resource;
    private ResourceType resourceType;
    private ResourceResourceType resourceResourceType;

    @Before
    public void setUp() {
        resource = mock(Resource.class);
        resourceType = mock(ResourceType.class);
        resourceResourceType = ResourceResourceType.create(resource, resourceType);

        verify(resource).addResourceResourceType(resourceResourceType);
        verify(resourceType).addResourceResourceType(resourceResourceType);
    }

    @Test
    public void getResource() {
        assertSame(resource, resourceResourceType.getResource());
    }

    @Test
    public void getResourceType() {
        assertSame(resourceType, resourceResourceType.getResourceType());
    }
}