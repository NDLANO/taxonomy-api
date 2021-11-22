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

public class NodeTranslationTest {
    private NodeTranslation entity;

    @BeforeEach
    public void setUp() {
        entity = new NodeTranslation();
    }

    @Test
    public void testConstructor() {
        var node = mock(Node.class);
        var nodeTranslation = new NodeTranslation(node, "en");
        assertEquals("en", nodeTranslation.getLanguageCode());
        assertEquals(node, nodeTranslation.getNode());
        verify(node).addTranslation(nodeTranslation);
    }

    @Test
    public void getAndSetTopic() {
        var node1 = mock(Node.class);
        var node2 = mock(Node.class);

        when(node1.getTranslations()).thenReturn(Set.of());
        when(node2.getTranslations()).thenReturn(Set.of());

        entity.setNode(node1);
        verify(node1).addTranslation(entity);

        when(node1.getTranslations()).thenReturn(Set.of(entity));

        entity.setNode(node2);
        verify(node2).addTranslation(entity);
        verify(node1).removeTranslation(entity);
    }

    @Test
    public void setAndGetName() {
        assertNull(entity.getName());

        entity.setName("test1");
        assertEquals("test1", entity.getName());
    }

    @Test
    public void getLanguageCode() {
        setField(entity, "languageCode", "nb");
        assertEquals("nb", entity.getLanguageCode());
    }
}
