package no.ndla.taxonomy.domain;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ResourceResourceTypeTest {
    private ResourceResourceType resourceResourceType;

    @Before
    public void setUp() {
        resourceResourceType = new ResourceResourceType();
    }

    @Test
    public void testConstructor() {
        final var resource = mock(Resource.class);
        final var resourceType = mock(ResourceType.class);

        final var resourceResourceType2 = new ResourceResourceType(resource, resourceType);
        assertEquals(resource, resourceResourceType2.getResource());
        assertEquals(resourceType, resourceResourceType2.getResourceType());
        assertNotNull(resourceResourceType2.getPublicId());
        assertTrue(resourceResourceType2.getPublicId().toString().length() > 4);
    }

    @Test
    public void setAndGetResource() {
        final var resource1 = mock(Resource.class);
        final var resource2 = mock(Resource.class);

        assertNull(resourceResourceType.getResource());

        resourceResourceType.setResource(resource1);
        verify(resource1).addResourceResourceType(resourceResourceType);
        assertEquals(resource1, resourceResourceType.getResource());

        resourceResourceType.setResource(resource2);
        verify(resource1).removeResourceResourceType(resourceResourceType);
        verify(resource2).addResourceResourceType(resourceResourceType);
        assertEquals(resource2, resourceResourceType.getResource());
    }

    @Test
    public void setAndGetResourceType() {
        final var resourceType1 = mock(ResourceType.class);
        final var resourceType2 = mock(ResourceType.class);

        assertNull(resourceResourceType.getResourceType());

        resourceResourceType.setResourceType(resourceType1);
        verify(resourceType1).addResourceResourceType(resourceResourceType);
        assertEquals(resourceType1, resourceResourceType.getResourceType());

        resourceResourceType.setResourceType(resourceType2);
        verify(resourceType1).removeResourceResourceType(resourceResourceType);
        verify(resourceType2).addResourceResourceType(resourceResourceType);
        assertEquals(resourceType2, resourceResourceType.getResourceType());
    }
}