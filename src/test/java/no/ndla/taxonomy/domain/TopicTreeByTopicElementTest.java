package no.ndla.taxonomy.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.util.ReflectionTestUtils.setField;

public class TopicTreeByTopicElementTest {
    private TopicTreeByTopicElement topicTreeByTopicElement;

    @BeforeEach
    public void setUp() {
        topicTreeByTopicElement = new TopicTreeByTopicElement();
        setField(topicTreeByTopicElement, "id", "2-0-0");
        setField(topicTreeByTopicElement, "rootTopicId", 205);
        setField(topicTreeByTopicElement, "topicId", 210);
        setField(topicTreeByTopicElement, "parentTopicId", 215);
        setField(topicTreeByTopicElement, "topicRank", 220);
        setField(topicTreeByTopicElement, "topicLevel", 225);
    }

    @Test
    public void getId() {
        assertEquals("2-0-0", topicTreeByTopicElement.getId());
    }

    @Test
    public void getRootTopicId() {
        assertEquals(205, topicTreeByTopicElement.getRootTopicId());
    }

    @Test
    public void getTopicId() {
        assertEquals(210, topicTreeByTopicElement.getTopicId());
    }

    @Test
    public void getParentTopicId() {
        assertEquals(215, topicTreeByTopicElement.getParentTopicId());
    }

    @Test
    public void getTopicRank() {
        assertEquals(220, topicTreeByTopicElement.getTopicRank());
    }

    @Test
    public void getTopicLevel() {
        assertEquals(225, topicTreeByTopicElement.getTopicLevel());
    }
}