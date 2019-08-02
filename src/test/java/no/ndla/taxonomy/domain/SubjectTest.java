package no.ndla.taxonomy.domain;

import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SubjectTest {
    private Subject subject;

    @Before
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

        subject.addSubjectTopic(subjectTopic1);
        assertEquals(1, subject.getSubjectTopics().size());
        assertTrue(subject.getSubjectTopics().contains(subjectTopic1));
        verify(subjectTopic1).setSubject(subject);

        when(subjectTopic1.getSubject()).thenReturn(Optional.of(subject));

        subject.addSubjectTopic(subjectTopic2);
        assertEquals(2, subject.getSubjectTopics().size());
        assertTrue(subject.getSubjectTopics().containsAll(Set.of(subjectTopic1, subjectTopic2)));
        verify(subjectTopic2).setSubject(subject);

        when(subjectTopic2.getSubject()).thenReturn(Optional.of(subject));

        subject.removeSubjectTopic(subjectTopic1);
        assertEquals(1, subject.getSubjectTopics().size());
        assertTrue(subject.getSubjectTopics().contains(subjectTopic2));
        verify(subjectTopic1).setSubject(null);

        subject.removeSubjectTopic(subjectTopic2);
        verify(subjectTopic2).setSubject(null);
        assertEquals(0, subject.getSubjectTopics().size());
    }

    @Test
    public void getAddAndRemoveTopic() {
        // This test is leaking to SubjectTopic causing some issues with relation management
        // Some of the whens and answers are only to replicate some behaviour that SubjectTopic require

        // Has "single subject"
        final var topic1 = mock(Topic.class);
        when(topic1.hasSingleSubject()).thenReturn(true);

        // Has "multiple subjects" (actually none, but returns false on hasSingleSubject)
        final var topic2 = mock(Topic.class);
        when(topic2.hasSingleSubject()).thenReturn(false);

        assertEquals(0, subject.getTopics().size());
        doAnswer(invocationOnMock -> {
            final var subjectTopic = (SubjectTopic) invocationOnMock.getArgument(0);
            assertEquals(topic1, subjectTopic.getTopic().orElse(null));
            assertEquals(subject, subjectTopic.getSubject().orElse(null));

            return null;
        }).when(topic1).addSubjectTopic(any(SubjectTopic.class));

        doAnswer(invocationOnMock -> {
            subject.getSubjectTopics().stream()
                    .filter(subjectTopic -> topic1.equals(subjectTopic.getTopic().orElse(null)))
                    .forEach(subjectTopic -> subjectTopic.setPrimary(true));

            return null;
        }).when(topic1).setPrimarySubject(subject);

        subject.addTopic(topic1);

        verify(topic1, times(1)).addSubjectTopic(any(SubjectTopic.class));
        verify(topic1, times(1)).setPrimarySubject(subject);

        assertEquals(1, subject.getTopics().size());
        assertTrue(subject.getTopics().contains(topic1));

        doAnswer(invocationOnMock -> {
            final var subjectTopic = (SubjectTopic) invocationOnMock.getArgument(0);
            assertEquals(topic2, subjectTopic.getTopic().orElse(null));
            assertEquals(subject, subjectTopic.getSubject().orElse(null));

            return null;
        }).when(topic2).addSubjectTopic(any(SubjectTopic.class));

        subject.addTopic(topic2);
        verify(topic2, times(0)).setRandomPrimarySubject();

        assertEquals(2, subject.getTopics().size());
        assertTrue(subject.getTopics().containsAll(Set.of(topic1, topic2)));

        // Trying to add again triggers exception
        try {
            subject.addTopic(topic2);
            fail("Expected DuplicateIdException");
        } catch (DuplicateIdException ignored) {

        }

        verify(topic1, times(0)).setRandomPrimarySubject();
        subject.removeTopic(topic1);

        assertEquals(1, subject.getTopics().size());
        assertTrue(subject.getTopics().contains(topic2));

        subject.removeTopic(topic2);

        assertEquals(0, subject.getTopics().size());

        // trying to delete again triggers exception
        try {
            subject.removeTopic(topic1);
            fail("Expected ChildNotFoundException");
        } catch (ChildNotFoundException ignored) {

        }
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
            subject.addSubjectTopic(subjectTopic);
            when(subjectTopic.getSubject()).thenReturn(Optional.of(subject));
        });

        assertEquals(2, subject.getSubjectTopics().size());

        subject.preRemove();

        assertEquals(0, subject.getSubjectTopics().size());

        Set.of(subjectTopic1, subjectTopic2).forEach(subjectTopic -> verify(subjectTopic).setSubject(null));
    }
}