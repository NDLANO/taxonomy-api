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
    private Topic subject;

    @BeforeEach
    public void setUp() {
        subject = new Topic();
    }

    @Test
    public void testConstructor() {
        assertNotNull(subject.getPublicId());
        assertTrue(subject.getPublicId().toString().length() > 4);
    }

    @Test
    public void addGetAndRemoveSubjectTopic() {
        final var subjectTopic1 = mock(TopicSubtopic.class);
        final var subjectTopic2 = mock(TopicSubtopic.class);

        assertEquals(0, subject.getChildConnections().size());

        try {
            subject.addChildTopicSubtopic(subjectTopic1);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {

        }

        when(subjectTopic1.getTopic()).thenReturn(Optional.of(subject));

        subject.addChildTopicSubtopic(subjectTopic1);

        assertEquals(1, subject.getChildConnections().size());
        assertTrue(subject.getChildrenTopicSubtopics().contains(subjectTopic1));

        when(subjectTopic1.getTopic()).thenReturn(Optional.of(subject));

        try {
            subject.addChildTopicSubtopic(subjectTopic2);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }

        when(subjectTopic2.getTopic()).thenReturn(Optional.of(subject));
        subject.addChildTopicSubtopic(subjectTopic2);

        assertEquals(2, subject.getChildConnections().size());
        assertTrue(subject.getChildConnections().containsAll(Set.of(subjectTopic1, subjectTopic2)));

        when(subjectTopic2.getTopic()).thenReturn(Optional.of(subject));

        subject.removeChildTopicSubTopic(subjectTopic1);
        assertEquals(1, subject.getChildConnections().size());
        assertTrue(subject.getChildConnections().contains(subjectTopic2));
        verify(subjectTopic1).disassociate();

        subject.removeChildTopicSubTopic(subjectTopic2);
        verify(subjectTopic2).disassociate();
        assertEquals(0, subject.getChildConnections().size());
    }

    @Test
    public void addAndGetAndRemoveTranslation() {
        assertEquals(0, subject.getTranslations().size());

        var returnedTranslation = subject.addTranslation("nb");
        assertEquals(1, subject.getTranslations().size());
        assertEquals("nb", returnedTranslation.getLanguageCode());
        assertTrue(subject.getTranslations().contains(returnedTranslation));
        assertEquals(subject, returnedTranslation.getTopic());

        var returnedTranslation2 = subject.addTranslation("en");
        assertEquals(2, subject.getTranslations().size());
        assertEquals("en", returnedTranslation2.getLanguageCode());
        assertTrue(subject.getTranslations().containsAll(Set.of(returnedTranslation, returnedTranslation2)));
        assertEquals(subject, returnedTranslation2.getTopic());

        subject.removeTranslation("nb");

        assertNull(returnedTranslation.getTopic());
        assertFalse(subject.getTranslations().contains(returnedTranslation));

        assertFalse(subject.getTranslation("nb").isPresent());

        subject.addTranslation(returnedTranslation);
        assertEquals(subject, returnedTranslation.getTopic());
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
        final var subjectTopic1 = mock(TopicSubtopic.class);
        final var subjectTopic2 = mock(TopicSubtopic.class);

        Set.of(subjectTopic1, subjectTopic2).forEach(subjectTopic -> {
            when(subjectTopic.getTopic()).thenReturn(Optional.of(subject));
            subject.addChildTopicSubtopic(subjectTopic);
            when(subjectTopic.getTopic()).thenReturn(Optional.of(subject));
        });

        assertEquals(2, subject.getChildConnections().size());

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