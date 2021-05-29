package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.repositories.TopicRepository;
import no.ndla.taxonomy.repositories.TopicSubtopicRepository;
import no.ndla.taxonomy.service.dtos.MetadataDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.*;
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
    private TopicSubtopicRepository topicSubtopicRepository;

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

    @Test
    public void testStartRecursiveCopy() {
        {
            final var resourceType = builder.resourceType(typeBuilder -> typeBuilder.publicId("urn:resourcetype:test"));
            final var topic = builder.topic(topicBuilder -> topicBuilder.publicId("urn:topic:orig-root").name("Name"));
            topic.addResourceType(resourceType);
            topic.addTranslation("nn").setName("Test");
            topicRepository.save(topic);
        }
        {
            final var metadataDto = new MetadataDto();
            metadataDto.setPublicId("urn:topic:orig-root");
            metadataDto.setVisible(true);
            metadataDto.setGrepCodes(Set.of("GREP-1"));
            metadataDto.setCustomFields(Map.of("other", "other"));
            when(metadataApiService.getMetadataByPublicId(URI.create("urn:topic:orig-root"))).thenReturn(metadataDto);
        }
        when(metadataApiService.updateMetadataByPublicId(any(URI.class), any(MetadataDto.class))).thenAnswer(inv -> {
            URI publicId = inv.getArgument(0);
            MetadataDto newMetadata = inv.getArgument(1);
            assertTrue(newMetadata.isVisible());
            assertEquals(Set.of("GREP-1"), newMetadata.getGrepCodes());
            assertNotNull(newMetadata.getCustomFields());
            assertEquals(publicId.toString(), newMetadata.getCustomFields().get("copy-root"));
            assertEquals("urn:topic:orig-root", newMetadata.getCustomFields().get("copy-of"));
            assertEquals("incomplete", newMetadata.getCustomFields().get("status"));
            assertEquals("other", newMetadata.getCustomFields().get("other"));
            return newMetadata;
        });

        // Make the call
        final var newTopicDto = topicService.prepareRecursiveCopy(URI.create("urn:topic:orig-root"));

        // Check response makes sense
        assertNotNull(newTopicDto);
        assertNotNull(newTopicDto.getId());

        // Examine the created topic object
        final var topic = topicRepository.findByPublicId(newTopicDto.getId());
        assertNotNull(topic);
        assertEquals("Name", topic.getName());
        assertNotNull(topic.getTopicResourceTypes());
        final var typeIterator = topic.getTopicResourceTypes().iterator();
        assertTrue(typeIterator.hasNext()); // Has an item
        final var topicResourceType = typeIterator.next(); // Get the item
        assertFalse(typeIterator.hasNext()); // Had only one single item
        assertEquals("urn:resourcetype:test", topicResourceType.getResourceType().map(ResourceType::getPublicId).map(URI::toString).orElse(null));
        assertNotNull(topic.getTranslations());
        final var translationsIterator = topic.getTranslations().iterator();
        assertTrue(translationsIterator.hasNext());
        final var translation = translationsIterator.next();
        assertFalse(translationsIterator.hasNext());
        assertEquals("nn", translation.getLanguageCode());
        assertEquals("Test", translation.getName());

        // Verify that metadata was set for the new topic
        verify(metadataApiService, times(1)).updateMetadataByPublicId(eq(newTopicDto.getId()), any(MetadataDto.class));
    }

    @Test
    public void testDoCopy() {
        {
            Topic oldRoot = builder.topic(b -> b.publicId("urn:topic:orig-root").subtopic(
                    sub -> sub.name("Sub").translation("nn", tr -> tr.name("Subb")).subtopic(subSub -> subSub.name("SubSub").resource(
                            resource -> resource.name("Resource")
                    ))
            ));
        }
        {
            Topic oldRoot = topicRepository.findByPublicId(URI.create("urn:topic:orig-root"));
            oldRoot.getChildrenTopicSubtopics().forEach(topicSubtopic -> {
                topicSubtopic.setRelevance(builder.relevance(r -> r.name("AssocRel")));
                topicSubtopicRepository.save(topicSubtopic);
            });
            oldRoot.getChildrenTopicSubtopics().forEach(topicSubtopic -> {
                topicSubtopic.getSubtopic().ifPresent(topic -> {
                    topic.addResourceType(builder.resourceType(rt -> rt.name("ResType")));
                    topicRepository.save(topic);
                });
            });
        }
        Topic newRoot = builder.topic(b -> b.publicId("urn:topic:new-root"));

        when(metadataApiService.updateMetadataByPublicId(eq(URI.create("urn:topic:new-root")), any(MetadataDto.class)))
                .thenAnswer(inv -> {
                    MetadataDto newMetadata = inv.getArgument(1);
                    assertTrue(newMetadata.isVisible());
                    assertEquals(Set.of("GREP-1"), newMetadata.getGrepCodes());
                    assertNotNull(newMetadata.getCustomFields());
                    assertEquals("urn:topic:orig-root", newMetadata.getCustomFields().get("copy-of"));
                    assertEquals("complete", newMetadata.getCustomFields().get("status"));
                    assertEquals("other", newMetadata.getCustomFields().get("other"));
                    return newMetadata;
                });
        when(metadataApiService.getMetadataByPublicId(any(URI.class)))
                .thenAnswer(inv -> {
                    URI publicId = inv.getArgument(0);
                    if (URI.create("urn:topic:new-root").equals(publicId)) {
                        MetadataDto prevMetadata = new MetadataDto();
                        prevMetadata.setVisible(true);
                        prevMetadata.setGrepCodes(new HashSet<>(Set.of("GREP-1")));
                        prevMetadata.setCustomFields(new HashMap<>(
                                Map.of(
                                        "copy-of", "urn:topic:orig-root",
                                        "status", "incomplete",
                                        "other", "other"
                                )
                        ));
                        return prevMetadata;
                    } else {
                        Topic topic = topicRepository.findByPublicId(publicId);
                        assertNotNull(topic);
                        MetadataDto metadataDto = new MetadataDto();
                        metadataDto.setCustomFields(Map.of("name", topic.getName()));
                        return metadataDto;
                    }
                });
        when(metadataApiService.updateMetadataByPublicId(any(URI.class), any(MetadataDto.class)))
                .thenAnswer(inv -> {
                    URI publicId = inv.getArgument(0);
                    MetadataDto newMetadata = inv.getArgument(1);
                    Topic topic = topicRepository.findByPublicId(publicId);
                    assertNotNull(topic);
                    assertNotNull(newMetadata.getCustomFields());
                    if ("incomplete".equals(newMetadata.getCustomFields().get("status"))) {
                        assertEquals("urn:topic:orig-root", newMetadata.getCustomFields().get("copy-of"));
                    } else {
                        assertEquals("complete", newMetadata.getCustomFields().get("status"));
                    }
                    assertEquals(topic.getName(), newMetadata.getCustomFields().get("name"));
                    return newMetadata;
                });

        topicService.runRecursiveCopy(URI.create("urn:topic:orig-root"), newRoot.getPublicId());
        newRoot = topicRepository.findByPublicId(newRoot.getPublicId());
        assertNotNull(newRoot);
        assertNotNull(newRoot.getChildrenTopicSubtopics());
        final var rootChildren = newRoot.getChildrenTopicSubtopics().iterator();
        assertTrue(rootChildren.hasNext());
        final var rootChildConnection = rootChildren.next();
        assertFalse(rootChildren.hasNext());

        {
            Relevance rel = rootChildConnection.getRelevance().orElse(null);
            assertNotNull(rel);
            assertEquals("AssocRel", rel.getName());
        }
        Topic sub = rootChildConnection.getSubtopic().orElse(null);
        assertNotNull(sub);
        assertEquals("Sub", sub.getName());
        assertEquals("Subb", sub.getTranslation("nn").map(TopicTranslation::getName).orElse(null));
        {
            assertNotNull(sub.getTopicResourceTypes());
            final var topicResourceTypes = sub.getTopicResourceTypes().iterator();
            assertTrue(topicResourceTypes.hasNext());
            final var topicResourceType = topicResourceTypes.next();
            assertFalse(topicResourceTypes.hasNext());
            final var resourceType = topicResourceType.getResourceType().orElse(null);
            assertNotNull(resourceType);
            assertEquals("ResType", resourceType.getName());
        }

        assertNotNull(sub.getChildrenTopicSubtopics());
        final var subChildren = sub.getChildrenTopicSubtopics().iterator();
        assertTrue(subChildren.hasNext());
        final var subChildConnection = subChildren.next();
        assertFalse(subChildren.hasNext());

        Topic subSub = subChildConnection.getSubtopic().orElse(null);
        assertNotNull(subSub);
        assertEquals("SubSub", subSub.getName());

        assertNotNull(subSub.getTopicResources());
        final var resourceConnections = subSub.getTopicResources().iterator();
        assertTrue(resourceConnections.hasNext());
        final var resourceConnection = resourceConnections.next();
        assertFalse(resourceConnections.hasNext());
        final var resource = resourceConnection.getResource().orElse(null);
        assertNotNull(resource);
        assertEquals("Resource", resource.getName());

        verify(metadataApiService, times(1)).updateMetadataByPublicId(eq(URI.create("urn:topic:new-root")), any(MetadataDto.class));
    }

    static class MockedSortedArrayList<E> extends ArrayList<E> {
        private MockedSortedArrayList(Collection<E> collection) {
            super(collection);
        }
    }
}