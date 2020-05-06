package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.repositories.FilterRepository;
import no.ndla.taxonomy.repositories.TopicRepository;
import no.ndla.taxonomy.repositories.TopicSubtopicRepository;
import no.ndla.taxonomy.service.dtos.MetadataDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@RunWith(SpringRunner.class)
@Transactional
public class TopicServiceImplTest {
    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private TopicSubtopicRepository topicSubtopicRepository;

    @Autowired
    private FilterRepository filterRepository;

    @Autowired
    private Builder builder;

    private EntityConnectionService entityConnectionService;

    private TopicServiceImpl topicService;

    private MetadataApiService metadataApiService;

    private MetadataEntityWrapperService metadataEntityWrapperService;

    @Before
    public void setUp() {
        entityConnectionService = mock(EntityConnectionService.class);
        metadataApiService = mock(MetadataApiService.class);
        metadataEntityWrapperService = mock(MetadataEntityWrapperService.class);

        topicService = new TopicServiceImpl(topicRepository, topicSubtopicRepository, filterRepository, entityConnectionService, metadataApiService, metadataEntityWrapperService);
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
            if (connection.connectionId.equals(URI.create("urn:subject-topic"))) {
                assertEquals("parent-subject", connection.type);
            } else if (connection.connectionId.equals(URI.create("urn:child-topic"))) {
                assertEquals("subtopic", connection.type);
            } else {
                fail();
            }
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    @Transactional
    public void getFilteredSubtopicConnections() {
        when(metadataEntityWrapperService.wrapEntities(anyList(), eq(false), any(Function.class))).thenAnswer(invocationOnMock -> {
            final var list = (List<DomainEntity>) invocationOnMock.getArgument(0);

            return list.stream()
                    .map(entity -> {
                        final var wrapped = mock(MetadataWrappedEntity.class);
                        when(wrapped.getEntity()).thenReturn(entity);

                        return wrapped;
                    })
                    .collect(Collectors.toList());
        });

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

        when(metadataEntityWrapperService.wrapEntities(anyList(), eq(true), any(Function.class))).thenAnswer(invocationOnMock -> {
            final var list = (List<DomainEntity>) invocationOnMock.getArgument(0);
            final var idGet = (Function) invocationOnMock.getArgument(2);

            return list.stream()
                    .map(entity -> {
                        final var wrapped = mock(MetadataWrappedEntity.class);
                        when(wrapped.getEntity()).thenReturn(entity);

                        final var wrappedId = (URI) idGet.apply(entity);

                        if (wrappedId.equals(topic1Id)) {
                            when(wrapped.getMetadata()).thenReturn(Optional.of(metadataObject1));
                        } else if (wrappedId.equals(topic2Id)) {
                            when(wrapped.getMetadata()).thenReturn(Optional.of(metadataObject2));
                        } else if (wrappedId.equals(topic3Id)) {
                            when(wrapped.getMetadata()).thenReturn(Optional.of(metadataObject3));
                        } else {
                            fail();
                        }

                        return wrapped;
                    })
                    .collect(Collectors.toList());
        });

        final var subtopicsWithNullFilters = topicService.getFilteredSubtopicConnections(topicId, (Set<URI>) null, "", false);
        final var subtopicsWithEmptyFilters = topicService.getFilteredSubtopicConnections(topicId, Set.of(), "", false);
        final var subtopicsWithEmptyFiltersAndMetadata = topicService.getFilteredSubtopicConnections(topicId, Set.of(), "", true);

        final var subtopicsByFilter1 = topicService.getFilteredSubtopicConnections(topicId, Set.of(filter1Id), "", false);
        final var subtopicsByFilter2 = topicService.getFilteredSubtopicConnections(topicId, Set.of(filter2Id), "", false);
        final var subtopicsByFilter3 = topicService.getFilteredSubtopicConnections(topicId, Set.of(filter3Id), "", false);
        final var subtopicsByFilter1And2 = topicService.getFilteredSubtopicConnections(topicId, Set.of(filter1Id, filter2Id), "", false);

        final var subtopicsBySubject1Filters = topicService.getFilteredSubtopicConnections(topicId, subject1Id, "", false);
        final var subtopicsBySubject2Filters = topicService.getFilteredSubtopicConnections(topicId, subject2Id, "", false);
        final var subtopicsBySubject3Filters = topicService.getFilteredSubtopicConnections(topicId, subject3Id, "", false);
        final var subtopicsWithNullSubjectId = topicService.getFilteredSubtopicConnections(topicId, (URI) null, "", false);

        assertEquals(3, subtopicsWithEmptyFilters.size());
        assertEquals(3, subtopicsWithNullFilters.size());
        assertEquals(3, subtopicsWithEmptyFiltersAndMetadata.size());

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
            assertTrue(allSubtopics.stream().map(document -> document.id).collect(Collectors.toSet()).containsAll(Set.of(topic1Id, topic2Id, topic3Id)));
        });

        // Collections with only topic1
        Set.of(subtopicsByFilter1, subtopicsBySubject1Filters).forEach(withFilter1 -> {
            assertTrue(withFilter1.stream().map(document -> document.id).collect(Collectors.toSet()).contains(topic1Id));
        });
        // Collections with only topic2
        Set.of(subtopicsByFilter2, subtopicsBySubject2Filters).forEach(withFilter2 -> {
            assertTrue(withFilter2.stream().map(document -> document.id).collect(Collectors.toSet()).contains(topic2Id));
        });

        // Check if metadata is present on the list that is supposed to include metadata
        subtopicsWithEmptyFiltersAndMetadata.forEach(subtopicDto -> {
            assertNotNull(subtopicDto.metadata);
            switch (subtopicDto.name) {
                case "topic1":
                    assertSame(metadataObject1, subtopicDto.metadata);
                    break;
                case "topic2":
                    assertSame(metadataObject2, subtopicDto.metadata);
                    break;
                case "topic3":
                    assertSame(metadataObject3, subtopicDto.metadata);
                    break;
                default:
                    fail("Unknown topic returned");
                    break;
            }
        });
    }
}