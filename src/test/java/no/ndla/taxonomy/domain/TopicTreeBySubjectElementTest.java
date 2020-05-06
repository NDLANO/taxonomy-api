package no.ndla.taxonomy.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.util.ReflectionTestUtils.setField;

public class TopicTreeBySubjectElementTest {
    private TopicTreeBySubjectElement topicTreeBySubjectElement;

    @BeforeEach
    public void setUp() {
        topicTreeBySubjectElement = new TopicTreeBySubjectElement();
        setField(topicTreeBySubjectElement, "topicId", 101);
        setField(topicTreeBySubjectElement, "connectionId", 102);
        setField(topicTreeBySubjectElement, "parentTopicId", 103);
        setField(topicTreeBySubjectElement, "topicRank", 104);
        setField(topicTreeBySubjectElement, "topicLevel", 105);
        setField(topicTreeBySubjectElement, "subjectId", 106);
    }

    @Test
    public void getTopicId() {
        assertEquals(101, topicTreeBySubjectElement.getTopicId());
    }

    @Test
    public void getConnectionId() {
        assertEquals(102, topicTreeBySubjectElement.getConnectionId());
    }

    @Test
    public void getParentTopicId() {
        assertEquals(103, topicTreeBySubjectElement.getParentTopicId());
    }

    @Test
    public void getTopicRank() {
        assertEquals(104, topicTreeBySubjectElement.getTopicRank());
    }

    @Test
    public void getTopicLevel() {
        assertEquals(105, topicTreeBySubjectElement.getTopicLevel());
    }

    @Test
    public void getSubjectId() {
        assertEquals(106, topicTreeBySubjectElement.getSubjectId());
    }
}