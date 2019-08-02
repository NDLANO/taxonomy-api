package no.ndla.taxonomy.domain;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SubjectTopicTest {
    private SubjectTopic subjectTopic;

    @Before
    public void setUp() {
        subjectTopic = new SubjectTopic();
    }

    @Test
    public void testConstructor() {
        final var subject = mock(Subject.class);
        final var topic = mock(Topic.class);

        final var createdSubjectTopic = new SubjectTopic(subject, topic);

        verify(subject).addSubjectTopic(createdSubjectTopic);
        verify(topic).addSubjectTopic(createdSubjectTopic);

        assertEquals(subject, createdSubjectTopic.getSubject().orElse(null));
        assertEquals(topic, createdSubjectTopic.getTopic().orElse(null));

        assertNotNull(createdSubjectTopic.getPublicId());
        assertTrue(createdSubjectTopic.getPublicId().toString().length() > 4);
    }

    @Test
    public void getAndSetSubject() {
        final var subject1 = mock(Subject.class);
        final var subject2 = mock(Subject.class);

        assertFalse(subjectTopic.getSubject().isPresent());

        subjectTopic.setSubject(subject1);
        verify(subject1).addSubjectTopic(subjectTopic);
        assertEquals(subject1, subjectTopic.getSubject().orElse(null));

        subjectTopic.setSubject(subject2);
        verify(subject1).removeSubjectTopic(subjectTopic);
        verify(subject2).addSubjectTopic(subjectTopic);
        assertEquals(subject2, subjectTopic.getSubject().orElse(null));
    }

    @Test
    public void getAndSetTopic() {
        final var topic1 = mock(Topic.class);
        final var topic2 = mock(Topic.class);

        assertFalse(subjectTopic.getTopic().isPresent());

        subjectTopic.setTopic(topic1);
        verify(topic1).addSubjectTopic(subjectTopic);
        assertEquals(topic1, subjectTopic.getTopic().orElse(null));

        subjectTopic.setTopic(topic2);
        verify(topic1).removeSubjectTopic(subjectTopic);
        verify(topic2).addSubjectTopic(subjectTopic);
        assertEquals(topic2, subjectTopic.getTopic().orElse(null));
    }

    @Test
    public void setPrimaryAndIsPrimary() {
        assertFalse(subjectTopic.isPrimary());
        subjectTopic.setPrimary(true);
        assertTrue(subjectTopic.isPrimary());
        subjectTopic.setPrimary(false);
        assertFalse(subjectTopic.isPrimary());
    }

    @Test
    public void getAndSetRank() {
        assertEquals(0, subjectTopic.getRank());
        subjectTopic.setRank(10);
        assertEquals(10, subjectTopic.getRank());
    }

    @Test
    public void preRemove() {
        final var topic = mock(Topic.class);
        final var subject = mock(Subject.class);

        subjectTopic.setTopic(topic);
        subjectTopic.setSubject(subject);

        assertEquals(topic, subjectTopic.getTopic().orElse(null));
        assertEquals(subject, subjectTopic.getSubject().orElse(null));

        subjectTopic.preRemove();

        assertFalse(subjectTopic.getSubject().isPresent());
        assertFalse(subjectTopic.getTopic().isPresent());

        verify(subject).removeSubjectTopic(subjectTopic);
        verify(topic).removeSubjectTopic(subjectTopic);
    }
}