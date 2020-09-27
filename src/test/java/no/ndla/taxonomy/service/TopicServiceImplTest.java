package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.repositories.TopicRepository;
import no.ndla.taxonomy.service.dtos.MetadataDto;
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
import java.util.stream.Collectors;

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
    private TopicTreeSorter topicTreeSorter;

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    @Transactional
    public void getFilteredSubtopicConnections() {
        when(topicTreeSorter.sortList(anyList())).thenAnswer(i -> new MockedSortedArrayList<>(i.getArgument(0)));

        final var subject1 = builder.subject();
        final var subject2 = builder.subject();
        final var subject3 = builder.subject();

        final var rootTopic = builder.topic(topicBuilder -> topicBuilder.name("root"));

        final var topic1 = builder.topic(topicBuilder -> topicBuilder.name("topic1"));
        final var topic2 = builder.topic(topicBuilder -> topicBuilder.name("topic2"));
        final var topic3 = builder.topic(topicBuilder -> topicBuilder.name("topic3"));

        final var filter1 = builder.filter(filterBuilder -> filterBuilder.subject(subject1));
        final var filter2 = builder.filter(filterBuilder -> filterBuilder.subject(subject2));
        final var filter3 = builder.filter(filterBuilder -> filterBuilder.subject(subject2));

        final var relevance = builder.relevance();

        TopicFilter.create(topic1, filter1, relevance);
        TopicFilter.create(topic2, filter2, relevance);

        SubjectTopic.create(subject1, rootTopic);

        TopicSubtopic.create(rootTopic, topic1);
        TopicSubtopic.create(rootTopic, topic2);
        TopicSubtopic.create(rootTopic, topic3);

        final var filter1Id = filter1.getPublicId();
        final var filter2Id = filter2.getPublicId();
        final var filter3Id = filter3.getPublicId();

        final var subject1Id = subject1.getPublicId();
        final var subject2Id = subject2.getPublicId();
        final var subject3Id = subject3.getPublicId();

        final var topic1Id = topic1.getPublicId();
        final var topic2Id = topic2.getPublicId();
        final var topic3Id = topic3.getPublicId();

        final var topicId = rootTopic.getPublicId();

        final var metadataObject1 = mock(MetadataDto.class);
        final var metadataObject2 = mock(MetadataDto.class);
        final var metadataObject3 = mock(MetadataDto.class);

        when(metadataObject1.getPublicId()).thenReturn(topic1Id.toString());
        when(metadataObject2.getPublicId()).thenReturn(topic2Id.toString());
        when(metadataObject3.getPublicId()).thenReturn(topic3Id.toString());

        when(metadataApiService.getMetadataByPublicId(anyCollection())).thenAnswer(invocationOnMock -> {
            final var list = (Collection<URI>) invocationOnMock.getArgument(0);

            return list.stream()
                    .map(publicId -> {
                        if (publicId.equals(topic1Id)) {
                            return metadataObject1;
                        } else if (publicId.equals(topic2Id)) {
                            return metadataObject2;
                        } else if (publicId.equals(topic3Id)) {
                            return metadataObject3;
                        } else {
                            fail();
                            return null;
                        }
                    })
                    .collect(Collectors.toSet());
        });

        final var subtopicsWithNullFilters = topicService.getFilteredSubtopicConnections(topicId, (Set<URI>) null, "");
        final var subtopicsWithEmptyFilters = topicService.getFilteredSubtopicConnections(topicId, Set.of(), "");

        final var subtopicsByFilter1 = topicService.getFilteredSubtopicConnections(topicId, Set.of(filter1Id), "");
        final var subtopicsByFilter2 = topicService.getFilteredSubtopicConnections(topicId, Set.of(filter2Id), "");
        final var subtopicsByFilter3 = topicService.getFilteredSubtopicConnections(topicId, Set.of(filter3Id), "");
        final var subtopicsByFilter1And2 = topicService.getFilteredSubtopicConnections(topicId, Set.of(filter1Id, filter2Id), "");

        final var subtopicsBySubject1Filters = topicService.getFilteredSubtopicConnections(topicId, subject1Id, "");
        final var subtopicsBySubject2Filters = topicService.getFilteredSubtopicConnections(topicId, subject2Id, "");
        final var subtopicsBySubject3Filters = topicService.getFilteredSubtopicConnections(topicId, subject3Id, "");
        final var subtopicsWithNullSubjectId = topicService.getFilteredSubtopicConnections(topicId, (URI) null, "");

        // Just tests that it was actually passed through sorting
        assertTrue(subtopicsByFilter1 instanceof MockedSortedArrayList);

        assertEquals(3, subtopicsWithEmptyFilters.size());
        assertEquals(3, subtopicsWithNullFilters.size());

        assertEquals(1, subtopicsByFilter1.size());
        assertEquals(1, subtopicsByFilter2.size());
        assertEquals(0, subtopicsByFilter3.size());
        assertEquals(2, subtopicsByFilter1And2.size());

        assertEquals(1, subtopicsBySubject1Filters.size());
        assertEquals(1, subtopicsBySubject2Filters.size());

        // Subject3 has no filters, it is equal to providing no filters to the method in itself, meaning no filtering
        assertEquals(3, subtopicsBySubject3Filters.size());

        assertEquals(3, subtopicsWithNullSubjectId.size());

        // Collections that should contain all objects
        Set.of(subtopicsWithNullFilters, subtopicsWithEmptyFilters, subtopicsBySubject3Filters, subtopicsWithNullSubjectId).forEach(allSubtopics -> {
            assertTrue(allSubtopics.stream().map(document -> document.getId()).collect(Collectors.toSet()).containsAll(Set.of(topic1Id, topic2Id, topic3Id)));
        });

        // Collections with only topic1
        Set.of(subtopicsByFilter1, subtopicsBySubject1Filters).forEach(withFilter1 -> {
            assertTrue(withFilter1.stream().map(document -> document.getId()).collect(Collectors.toSet()).contains(topic1Id));
        });
        // Collections with only topic2
        Set.of(subtopicsByFilter2, subtopicsBySubject2Filters).forEach(withFilter2 -> {
            assertTrue(withFilter2.stream().map(document -> document.getId()).collect(Collectors.toSet()).contains(topic2Id));
        });

        // Check if metadata is present on the list that is supposed to include metadata
        subtopicsWithEmptyFilters.forEach(subtopicDto -> {
            assertNotNull(subtopicDto.getMetadata());
            switch (subtopicDto.getName()) {
                case "topic1":
                    assertSame(metadataObject1, subtopicDto.getMetadata());
                    break;
                case "topic2":
                    assertSame(metadataObject2, subtopicDto.getMetadata());
                    break;
                case "topic3":
                    assertSame(metadataObject3, subtopicDto.getMetadata());
                    break;
                default:
                    fail("Unknown topic returned");
                    break;
            }
        });
    }

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