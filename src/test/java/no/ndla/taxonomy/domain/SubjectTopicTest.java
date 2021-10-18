/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SubjectTopicTest {
    private Subject subject;
    private Topic topic;
    private SubjectTopic subjectTopic;

    @BeforeEach
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
