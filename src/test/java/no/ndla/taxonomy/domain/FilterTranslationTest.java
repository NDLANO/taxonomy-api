package no.ndla.taxonomy.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

public class FilterTranslationTest {
    private FilterTranslation filterTranslation;

    @BeforeEach
    public void setUp() {
        filterTranslation = new FilterTranslation();
    }

    @Test
    public void testConstructor() {
        var filter = mock(Filter.class);
        var filterTranslation2 = new FilterTranslation(filter, "en");
        assertEquals("en", filterTranslation2.getLanguageCode());
        assertEquals(filter, filterTranslation2.getFilter());
        verify(filter).addTranslation(filterTranslation2);
    }

    @Test
    public void getAndSetFilter() {
        var filter1 = mock(Filter.class);
        var filter2 = mock(Filter.class);

        when(filter1.getTranslations()).thenReturn(Set.of());
        when(filter2.getTranslations()).thenReturn(Set.of());

        filterTranslation.setFilter(filter1);
        verify(filter1).addTranslation(filterTranslation);

        when(filter1.getTranslations()).thenReturn(Set.of(filterTranslation));

        filterTranslation.setFilter(filter2);
        verify(filter2).addTranslation(filterTranslation);
        verify(filter1).removeTranslation(filterTranslation);
    }

    @Test
    public void setAndGetName() {
        assertNull(filterTranslation.getName());

        filterTranslation.setName("test1");
        assertEquals("test1", filterTranslation.getName());
    }

    @Test
    public void getLanguageCode() {
        setField(filterTranslation, "languageCode", "nb");
        assertEquals("nb", filterTranslation.getLanguageCode());
    }
}