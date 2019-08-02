package no.ndla.taxonomy.domain;

import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

public class ResourceTypeTranslationTest {
    private ResourceTypeTranslation resourceTypeTranslation;

    @Before
    public void setUp() {
        resourceTypeTranslation = new ResourceTypeTranslation();
    }

    @Test
    public void testConstructor() {
        var resourceType = mock(ResourceType.class);
        var resourceTypeTranslation2 = new ResourceTypeTranslation(resourceType, "en");
        assertEquals("en", resourceTypeTranslation2.getLanguageCode());
        assertEquals(resourceType, resourceTypeTranslation2.getResourceType());
        verify(resourceType).addTranslation(resourceTypeTranslation2);
    }

    @Test
    public void getAndSetResourceType() {
        var resourceType1 = mock(ResourceType.class);
        var resourceType2 = mock(ResourceType.class);

        when(resourceType1.getTranslations()).thenReturn(Set.of());
        when(resourceType2.getTranslations()).thenReturn(Set.of());

        resourceTypeTranslation.setResourceType(resourceType1);
        verify(resourceType1).addTranslation(resourceTypeTranslation);

        when(resourceType1.getTranslations()).thenReturn(Set.of(resourceTypeTranslation));

        resourceTypeTranslation.setResourceType(resourceType2);
        verify(resourceType2).addTranslation(resourceTypeTranslation);
        verify(resourceType1).removeTranslation(resourceTypeTranslation);
    }

    @Test
    public void setAndGetName() {
        assertNull(resourceTypeTranslation.getName());

        resourceTypeTranslation.setName("test1");
        assertEquals("test1", resourceTypeTranslation.getName());
    }

    @Test
    public void getLanguageCode() {
        setField(resourceTypeTranslation, "languageCode", "nb");
        assertEquals("nb", resourceTypeTranslation.getLanguageCode());
    }
}