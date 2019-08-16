package no.ndla.taxonomy.domain;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SubjectTopicTest {
    private Subject subject;
    private Topic topic;
    private SubjectTopic subjectTopic;

    @Before
    public void setUp() {
        topic = mock(Topic.class);
        subject = mock(Subject.class);

        subjectTopic = SubjectTopic.create(subject, topic);
    }

    @Test
    public void getSubject() {
        assertSame(subject, subjectTopic.getSubject().orElse(null));
    }

    @Test
    public void getTopic() {
        assertSame(topic, subjectTopic.getTopic().orElse(null));
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
        subjectTopic.preRemove();

        assertFalse(subjectTopic.getSubject().isPresent());
        assertFalse(subjectTopic.getTopic().isPresent());

        verify(subject).removeSubjectTopic(subjectTopic);
        verify(topic).removeSubjectTopic(subjectTopic);
    }
}