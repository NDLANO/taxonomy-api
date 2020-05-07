package no.ndla.taxonomy.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

public class SubjectTest {
    private Subject subject;

    @BeforeEach
    public void setUp() {
        subject = new Subject();
    }

    @Test
    public void testConstructor() {
        assertNotNull(subject.getPublicId());
        assertTrue(subject.getPublicId().toString().length() > 4);
    }

    @Test
    public void addGetAndRemoveSubjectTopic() {
        final var subjectTopic1 = mock(SubjectTopic.class);
        final var subjectTopic2 = mock(SubjectTopic.class);

        assertEquals(0, subject.getSubjectTopics().size());

        try {
            subject.addSubjectTopic(subjectTopic1);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {

        }

        when(subjectTopic1.getSubject()).thenReturn(Optional.of(subject));

        subject.addSubjectTopic(subjectTopic1);

        assertEquals(1, subject.getSubjectTopics().size());
        assertTrue(subject.getSubjectTopics().contains(subjectTopic1));

        when(subjectTopic1.getSubject()).thenReturn(Optional.of(subject));

        try {
            subject.addSubjectTopic(subjectTopic2);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }

        when(subjectTopic2.getSubject()).thenReturn(Optional.of(subject));
        subject.addSubjectTopic(subjectTopic2);

        assertEquals(2, subject.getSubjectTopics().size());
        assertTrue(subject.getSubjectTopics().containsAll(Set.of(subjectTopic1, subjectTopic2)));

        when(subjectTopic2.getSubject()).thenReturn(Optional.of(subject));

        subject.removeSubjectTopic(subjectTopic1);
        assertEquals(1, subject.getSubjectTopics().size());
        assertTrue(subject.getSubjectTopics().contains(subjectTopic2));
        verify(subjectTopic1).disassociate();

        subject.removeSubjectTopic(subjectTopic2);
        verify(subjectTopic2).disassociate();
        assertEquals(0, subject.getSubjectTopics().size());
    }

    @Test
    public void getAddAndRemoveFilter() {
        final var filter1 = mock(Filter.class);
        final var filter2 = mock(Filter.class);

        assertEquals(0, subject.getFilters().size());

        subject.addFilter(filter1);
        verify(filter1).setSubject(subject);
        assertEquals(1, subject.getFilters().size());
        assertTrue(subject.getFilters().contains(filter1));

        subject.addFilter(filter2);
        verify(filter2).setSubject(subject);
        assertEquals(2, subject.getFilters().size());
        assertTrue(subject.getFilters().containsAll(Set.of(filter1, filter2)));

        Set.of(filter1, filter2).forEach(filter -> when(filter.getSubject()).thenReturn(Optional.of(subject)));

        subject.removeFilter(filter1);
        verify(filter1).setSubject(null);
        assertEquals(1, subject.getFilters().size());
        assertTrue(subject.getFilters().contains(filter2));

        subject.removeFilter(filter2);
        verify(filter2).setSubject(null);
        assertEquals(0, subject.getFilters().size());
    }

    @Test
    public void addAndGetAndRemoveTranslation() {
        assertEquals(0, subject.getTranslations().size());

        var returnedTranslation = subject.addTranslation("nb");
        assertEquals(1, subject.getTranslations().size());
        assertEquals("nb", returnedTranslation.getLanguageCode());
        assertTrue(subject.getTranslations().contains(returnedTranslation));
        assertEquals(subject, returnedTranslation.getSubject());

        var returnedTranslation2 = subject.addTranslation("en");
        assertEquals(2, subject.getTranslations().size());
        assertEquals("en", returnedTranslation2.getLanguageCode());
        assertTrue(subject.getTranslations().containsAll(Set.of(returnedTranslation, returnedTranslation2)));
        assertEquals(subject, returnedTranslation2.getSubject());

        subject.removeTranslation("nb");

        assertNull(returnedTranslation.getSubject());
        assertFalse(subject.getTranslations().contains(returnedTranslation));

        assertFalse(subject.getTranslation("nb").isPresent());

        subject.addTranslation(returnedTranslation);
        assertEquals(subject, returnedTranslation.getSubject());
        assertTrue(subject.getTranslations().contains(returnedTranslation));

        assertEquals(returnedTranslation, subject.getTranslation("nb").get());
        assertEquals(returnedTranslation2, subject.getTranslation("en").orElse(null));
    }

    @Test
    public void name() {
        assertEquals(subject, subject.name("test name"));
        assertEquals("test name", subject.getName());
    }

    @Test
    public void setAndGetContentUri() throws URISyntaxException {
        assertNull(subject.getContentUri());
        subject.setContentUri(new URI("urn:testuri1"));
        assertEquals("urn:testuri1", subject.getContentUri().toString());
    }

    @Test
    public void preRemove() {
        final var subjectTopic1 = mock(SubjectTopic.class);
        final var subjectTopic2 = mock(SubjectTopic.class);

        Set.of(subjectTopic1, subjectTopic2).forEach(subjectTopic -> {
            when(subjectTopic.getSubject()).thenReturn(Optional.of(subject));
            subject.addSubjectTopic(subjectTopic);
            when(subjectTopic.getSubject()).thenReturn(Optional.of(subject));
        });

        assertEquals(2, subject.getSubjectTopics().size());

        subject.preRemove();

        Set.of(subjectTopic1, subjectTopic2).forEach(subjectTopic -> verify(subjectTopic).disassociate());
    }

    @Test
    public void isContext() {
        final var subject = new Subject();
        assertTrue(subject.isContext());
    }

    @Test
    public void getCachedPaths() {
        final var cachedPaths = Set.of();

        setField(subject, "cachedPaths", cachedPaths);
        assertSame(cachedPaths, subject.getCachedPaths());
    }
}