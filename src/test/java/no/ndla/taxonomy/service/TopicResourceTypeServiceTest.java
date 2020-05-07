package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.ResourceType;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.domain.TopicResourceType;
import no.ndla.taxonomy.repositories.ResourceTypeRepository;
import no.ndla.taxonomy.repositories.TopicRepository;
import no.ndla.taxonomy.repositories.TopicResourceTypeRepository;
import no.ndla.taxonomy.service.exceptions.InvalidArgumentServiceException;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

public class TopicResourceTypeServiceTest {
    private TopicRepository topicRepository;
    private ResourceTypeRepository resourceTypeRepository;
    private TopicResourceTypeRepository topicResourceTypeRepository;
    private TopicResourceTypeService topicResourceTypeService;

    @BeforeEach
    public void beforeTesting() {
        topicRepository = mock(TopicRepository.class);
        resourceTypeRepository = mock(ResourceTypeRepository.class);
        topicResourceTypeRepository = mock(TopicResourceTypeRepository.class);
        topicResourceTypeService = new TopicResourceTypeService(topicRepository, resourceTypeRepository, topicResourceTypeRepository);
    }

    @Test
    public void testTopicNotSpecified() {
        assertThrows(InvalidArgumentServiceException.class, () -> topicResourceTypeService.getTopicResourceTypes(null));
    }

    @Test
    public void testTopicNotFound() throws URISyntaxException {
        try {
            topicResourceTypeService.getTopicResourceTypes(new URI("urn:topic:1"));
            fail("Expected NotFoundServiceException");
        } catch (NotFoundServiceException ignored) {
        }

        verify(topicRepository, times(1)).findByPublicId(new URI("urn:topic:1"));
    }

    @Test
    public void testNoResourceTypes() throws URISyntaxException {
        Topic topic = new Topic();
        given(topicRepository.findByPublicId(new URI("urn:topic:1"))).willReturn(topic);
        assertEquals(List.of(), topicResourceTypeService.getTopicResourceTypes(new URI("urn:topic:1")));
        verify(topicResourceTypeRepository, times(1)).findAllByTopic(topic);
    }

    @Test
    public void testResourceTypes() throws URISyntaxException {
        Topic topic = new Topic();
        ResourceType resourceType = new ResourceType();
        var topicResourceType = TopicResourceType.create(topic, resourceType);
        given(topicRepository.findByPublicId(new URI("urn:topic:1"))).willReturn(topic);
        given(topicResourceTypeRepository.findAllByTopic(topic)).willReturn(List.of(topicResourceType));
        assertEquals(
                topicResourceType.getPublicId().toString(),
                topicResourceTypeService.getTopicResourceTypes(new URI("urn:topic:1"))
                        .stream()
                        .map(TopicResourceType::getPublicId)
                        .map(Object::toString)
                        .reduce((a, b) -> a + b)
                        .orElse(null)
        );
    }

    @Test
    public void testAddResourceTypeWithTopicIdNull() {
        assertThrows(InvalidArgumentServiceException.class, () -> topicResourceTypeService.addTopicResourceType(null, new URI("urn:resourcetype:1")));
    }

    @Test
    public void testAddResourceTypeWithResourceTypeIdNull() {
        assertThrows(InvalidArgumentServiceException.class, () -> topicResourceTypeService.addTopicResourceType(new URI("urn:topic:1"), null));
    }

    @Test
    public void testAddResourceTypeWithTopicNotFound() throws URISyntaxException {
        assertThrows(NotFoundServiceException.class, () -> topicResourceTypeService.addTopicResourceType(new URI("urn:topic:1"), new URI("urn:resourcetype:1")));

        verify(topicRepository, times(1)).findByPublicId(new URI("urn:topic:1"));
        verify(resourceTypeRepository, never()).findByPublicId(new URI("urn:resourcetype:1"));
    }

    @Test
    public void testAddResourceTypeWithResourceTypeNotFound() throws URISyntaxException {
        Topic topic = new Topic();
        given(topicRepository.findByPublicId(new URI("urn:topic:1"))).willReturn(topic);

        assertThrows(NotFoundServiceException.class, () -> topicResourceTypeService.addTopicResourceType(new URI("urn:topic:1"), new URI("urn:resourcetype:1")));

        verify(topicRepository, times(1)).findByPublicId(new URI("urn:topic:1"));
        verify(resourceTypeRepository, times(1)).findByPublicId(new URI("urn:resourcetype:1"));
    }

    @Test
    public void testAddResourceType() throws URISyntaxException {
        Topic topic = spy(new Topic());
        given(topicRepository.findByPublicId(new URI("urn:topic:1"))).willReturn(topic);
        ResourceType resourceType = new ResourceType();
        given(resourceTypeRepository.findByPublicId(new URI("urn:resourcetype:1"))).willReturn(resourceType);
        topicResourceTypeService.addTopicResourceType(new URI("urn:topic:1"), new URI("urn:resourcetype:1"));
        verify(topic, times(1)).addResourceType(resourceType);
        verify(topicRepository, times(1)).save(topic);
    }

    @Test
    public void testDeleteTopicResourceTypeNull() {
        assertThrows(InvalidArgumentServiceException.class, () -> topicResourceTypeService.deleteTopicResourceType(null));
    }

    @Test
    public void testDeleteTopicResourceTypeNotFound() throws URISyntaxException {
        assertThrows(NotFoundServiceException.class, () -> topicResourceTypeService.deleteTopicResourceType(new URI("urn:topicresourcetype:1")));

        verify(topicResourceTypeRepository, times(1)).findByPublicId(new URI("urn:topicresourcetype:1"));
    }

    @Test
    public void testDeleteTopicResourceType() throws URISyntaxException {
        TopicResourceType topicResourceType = TopicResourceType.create(new Topic(), new ResourceType());
        given(topicResourceTypeRepository.findByPublicId(new URI("urn:topicresourcetype:1"))).willReturn(topicResourceType);
        topicResourceTypeService.deleteTopicResourceType(new URI("urn:topicresourcetype:1"));
        verify(topicResourceTypeRepository, times(1)).delete(topicResourceType);
    }

    @Test
    public void testFindAll() {
        Stream<TopicResourceType> stream = Stream.of();
        List<TopicResourceType> list = mock(List.class);
        given(list.stream()).willReturn(stream);
        given(topicResourceTypeRepository.findAll()).willReturn(list);
        assertSame(stream, topicResourceTypeService.findAll());
    }

    @Test
    public void testFindById() throws URISyntaxException {
        TopicResourceType topicResourceType = TopicResourceType.create(new Topic(), new ResourceType());
        given(topicResourceTypeRepository.findByPublicId(new URI("urn:topicresourcetype:1"))).willReturn(topicResourceType);
        assertEquals(topicResourceType, topicResourceTypeService.findById(new URI("urn:topicresourcetype:1")).orElse(null));
    }

    @Test
    public void testFindByIdNotFound() throws URISyntaxException {
        assertFalse(topicResourceTypeService.findById(new URI("urn:topicresourcetype:1")).isPresent());
    }
}
