package no.ndla.taxonomy.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SubjectTopicTest {
    private Topic subject;
    private Topic topic;
    private TopicSubtopic subjectTopic;

    @BeforeEach
    public void setUp() {
        topic = mock(Topic.class);
        subject = mock(Topic.class);

        subjectTopic = TopicSubtopic.create(subject, topic);
    }

    @Test
    public void getSubject() {
        assertSame(subject, subjectTopic.getTopic().orElse(null));
    }

    @Test
    public void getTopic() {
        assertSame(topic, subjectTopic.getSubtopic().orElse(null));
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

        assertFalse(subjectTopic.getTopic().isPresent());
        assertFalse(subjectTopic.getSubtopic().isPresent());

        verify(subject).removeChildTopicSubTopic(subjectTopic);
        verify(topic).removeParentTopicSubtopic(subjectTopic);
    }
}