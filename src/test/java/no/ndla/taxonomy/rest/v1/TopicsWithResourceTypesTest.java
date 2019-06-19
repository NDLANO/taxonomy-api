package no.ndla.taxonomy.rest.v1;

import no.ndla.taxonomy.domain.ResourceType;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.domain.TopicResourceType;
import no.ndla.taxonomy.service.TopicResourceTypeService;
import no.ndla.taxonomy.service.exceptions.InvalidArgumentServiceException;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.util.Optional;
import java.util.stream.Stream;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = TopicsWithResourceTypes.class)
public class TopicsWithResourceTypesTest {
    @MockBean
    private TopicResourceTypeService topicResourceTypeService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testAddTopicResourceTypeBadRequest() throws Exception {
        given(topicResourceTypeService.addTopicResourceType(new URI("urn:topic:1"), new URI("urn:resourcetype:1"))).willThrow(new InvalidArgumentServiceException("Bad request"));
        mockMvc.perform(
                post("http://localhost/v1/topic-resourcetypes")
                        .contentType("application/json")
                        .content("{\"topicId\": \"urn:topic:1\", \"resourceTypeId\": \"urn:resourcetype:1\"}")
        )
                .andExpect(status().isBadRequest());
        verify(topicResourceTypeService, times(1)).addTopicResourceType(new URI("urn:topic:1"), new URI("urn:resourcetype:1"));
    }

    @Test
    public void testAddTopicResourceTypeNotFound() throws Exception {
        given(topicResourceTypeService.addTopicResourceType(new URI("urn:topic:1"), new URI("urn:resourcetype:1"))).willThrow(new NotFoundServiceException("Not found"));
        mockMvc.perform(
                post("http://localhost/v1/topic-resourcetypes")
                        .contentType("application/json")
                        .content("{\"topicId\": \"urn:topic:1\", \"resourceTypeId\": \"urn:resourcetype:1\"}")
        )
                .andExpect(status().isNotFound());
        verify(topicResourceTypeService, times(1)).addTopicResourceType(new URI("urn:topic:1"), new URI("urn:resourcetype:1"));
    }

    @Test
    public void testAddTopicResourceType() throws Exception {
        given(topicResourceTypeService.addTopicResourceType(new URI("urn:topic:1"), new URI("urn:resourcetype:1"))).willReturn(new URI("urn:topic-resourcetype:1"));
        mockMvc.perform(
                post("http://localhost/v1/topic-resourcetypes")
                        .contentType("application/json")
                        .content("{\"topicId\": \"urn:topic:1\", \"resourceTypeId\": \"urn:resourcetype:1\"}")
        )
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/topic-resourcetypes/urn:topic-resourcetype:1"));
    }

    @Test
    public void testDeleteTopicResourceTypeBadRequest() throws Exception {
        doThrow(new InvalidArgumentServiceException("Bad request"))
                .when(topicResourceTypeService)
                .deleteTopicResourceType(new URI("urn:topic-resourcetype:1"));
        mockMvc.perform(delete("http://localhost/v1/topic-resourcetypes/urn:topic-resourcetype:1"))
                .andExpect(status().isBadRequest());
        verify(topicResourceTypeService, times(1)).deleteTopicResourceType(new URI("urn:topic-resourcetype:1"));
    }

    @Test
    public void testDeleteTopicResourceTypeNotFound() throws Exception {
        doThrow(new NotFoundServiceException("Not found"))
                .when(topicResourceTypeService)
                .deleteTopicResourceType(new URI("urn:topic-resourcetype:1"));
        mockMvc.perform(delete("http://localhost/v1/topic-resourcetypes/urn:topic-resourcetype:1"))
                .andExpect(status().isNotFound());
        verify(topicResourceTypeService, times(1)).deleteTopicResourceType(new URI("urn:topic-resourcetype:1"));
    }

    @Test
    public void testDeleteTopicResourceType() throws Exception {
        doThrow(new InvalidArgumentServiceException("Bad request"))
                .when(topicResourceTypeService)
                .deleteTopicResourceType(new URI("urn:topic-resourcetype:1"));
        mockMvc.perform(delete("http://localhost/v1/topic-resourcetypes/urn:topic-resourcetype:1"))
                .andExpect(status().isBadRequest());
        verify(topicResourceTypeService, times(1)).deleteTopicResourceType(new URI("urn:topic-resourcetype:1"));
    }

    @Test
    public void testListAllTopicResourceTypes() throws Exception {
        var topic = new Topic();
        var resourceType = new ResourceType();
        var topicResourceType = new TopicResourceType(topic, resourceType);
        var expectedJson = "{" +
                "\"topicId\": \""+topic.getPublicId().toString()+"\"," +
                "\"resourceTypeId\":\""+resourceType.getPublicId().toString()+"\"," +
                "\"id\":\""+topicResourceType.getPublicId().toString()+"\"" +
                "}";
        given(topicResourceTypeService.findAll()).willReturn(Stream.of(topicResourceType));
        mockMvc.perform(get("http://localhost/v1/topic-resourcetypes"))
                .andExpect(status().isOk())
                .andExpect(content().json("["+expectedJson+"]"));
    }

    @Test
    public void testFindOneTopicResourceTypeNotFound() throws Exception {
        given(topicResourceTypeService.findById(new URI("urn:topic-resourcetype:1"))).willReturn(Optional.empty());
        mockMvc.perform(get("http://localhost/v1/topic-resourcetypes/urn:topic-resourcetype:1"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testFindOneTopicResourceType() throws Exception {
        var topic = new Topic();
        var resourceType = new ResourceType();
        var topicResourceType = new TopicResourceType(topic, resourceType);
        var expectedJson = "{" +
                "\"topicId\": \""+topic.getPublicId().toString()+"\"," +
                "\"resourceTypeId\":\""+resourceType.getPublicId().toString()+"\"," +
                "\"id\":\""+topicResourceType.getPublicId().toString()+"\"" +
                "}";
        given(topicResourceTypeService.findById(new URI("urn:topic-resourcetype:1"))).willReturn(Optional.of(topicResourceType));
        mockMvc.perform(get("http://localhost/v1/topic-resourcetypes/urn:topic-resourcetype:1"))
                .andExpect(status().isOk())
                .andExpect(content().json(expectedJson));
    }
}
