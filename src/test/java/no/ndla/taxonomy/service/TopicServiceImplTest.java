/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.repositories.TopicRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@Transactional
public class TopicServiceImplTest {
    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private Builder builder;

    @MockBean
    private MetadataApiService metadataApiService;

    @MockBean
    private EntityConnectionService entityConnectionService;

    @Autowired
    private TopicServiceImpl topicService;

    @MockBean
    private TreeSorter topicTreeSorter;

    @Test
    public void delete() {
        final var createdTopic = builder.topic();
        final var topicId = createdTopic.getPublicId();

        topicService.delete(topicId);

        assertFalse(topicRepository.findFirstByPublicId(topicId).isPresent());
        verify(entityConnectionService).disconnectAllChildren(createdTopic);

        verify(metadataApiService).deleteMetadataByPublicId(topicId);
    }

    @Test
    public void getAllConnections() {
        final var topicId = builder.topic().getPublicId();

        final var subjectTopic = mock(SubjectTopic.class);
        final var parentTopicSubtopic = mock(TopicSubtopic.class);
        final var childTopicSubtopic = mock(TopicSubtopic.class);

        when(subjectTopic.getPublicId()).thenReturn(URI.create("urn:subject-topic"));
        when(parentTopicSubtopic.getPublicId()).thenReturn(URI.create("urn:parent-topic"));
        when(childTopicSubtopic.getPublicId()).thenReturn(URI.create("urn:child-topic"));

        final var parentConnectionsToReturn = Set.of(subjectTopic);
        final var childConnectionsToReturn = Set.of(childTopicSubtopic);

        when(entityConnectionService.getParentConnections(any(Topic.class))).thenAnswer(invocationOnMock -> {
            final var topic = (Topic) invocationOnMock.getArgument(0);
            assertEquals(topicId, topic.getPublicId());

            return parentConnectionsToReturn;
        });
        when(entityConnectionService.getChildConnections(any(Topic.class))).thenAnswer(invocationOnMock -> {
            final var topic = (Topic) invocationOnMock.getArgument(0);
            assertEquals(topicId, topic.getPublicId());

            return childConnectionsToReturn;
        });

        final var returnedConnections = topicService.getAllConnections(topicId);

        assertEquals(2, returnedConnections.size());
        returnedConnections.forEach(connection -> {
            if (connection.getConnectionId().equals(URI.create("urn:subject-topic"))) {
                assertEquals("parent-subject", connection.getType());
            } else if (connection.getConnectionId().equals(URI.create("urn:child-topic"))) {
                assertEquals("subtopic", connection.getType());
            } else {
                fail();
            }
        });
    }

    static class MockedSortedArrayList<E> extends ArrayList<E> {
        private MockedSortedArrayList(Collection<E> collection) {
            super(collection);
        }
    }
}
