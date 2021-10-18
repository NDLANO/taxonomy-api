/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

public class SubjectTranslationTest {
    private SubjectTranslation subjectTranslation;

    @BeforeEach
    public void setUp() {
        subjectTranslation = new SubjectTranslation();
    }

    @Test
    public void testConstructor() {
        var subject = mock(Subject.class);
        var subjectTranslation2 = new SubjectTranslation(subject, "en");
        assertEquals("en", subjectTranslation2.getLanguageCode());
        assertEquals(subject, subjectTranslation2.getSubject());
        verify(subject).addTranslation(subjectTranslation2);
    }

    @Test
    public void getAndSetSubject() {
        var subject1 = mock(Subject.class);
        var subject2 = mock(Subject.class);

        when(subject1.getTranslations()).thenReturn(Set.of());
        when(subject2.getTranslations()).thenReturn(Set.of());

        subjectTranslation.setSubject(subject1);
        verify(subject1).addTranslation(subjectTranslation);

        when(subject1.getTranslations()).thenReturn(Set.of(subjectTranslation));

        subjectTranslation.setSubject(subject2);
        verify(subject2).addTranslation(subjectTranslation);
        verify(subject1).removeTranslation(subjectTranslation);
    }

    @Test
    public void setAndGetName() {
        assertNull(subjectTranslation.getName());

        subjectTranslation.setName("test1");
        assertEquals("test1", subjectTranslation.getName());
    }

    @Test
    public void getLanguageCode() {
        setField(subjectTranslation, "languageCode", "nb");
        assertEquals("nb", subjectTranslation.getLanguageCode());
    }
}
