package no.ndla.taxonomy.rest.v1;

import no.ndla.taxonomy.domain.ResourceType;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.domain.TopicResourceType;
import no.ndla.taxonomy.repositories.*;
import no.ndla.taxonomy.service.TopicResourceTypeService;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = Topics.class)
public class TopicsWebTest {
    @MockBean
    TopicRepository topicRepository;
    @MockBean
    TopicSubtopicRepository topicSubtopicRepository;
    @MockBean
    SubjectTopicRepository subjectTopicRepository;
    @MockBean
    SubjectRepository subjectRepository;
    @MockBean
    JdbcTemplate jdbcTemplate;
    @MockBean
    TopicResourceTypeService topicResourceTypeService;
    @MockBean
    TopicResourceRepository topicResourceRepository;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testTopicNotFound() throws Exception {
        given(topicResourceTypeService.getTopicResourceTypes(new URI("urn:topic:1"))).willThrow(new NotFoundServiceException("Not found"));
        mockMvc.perform(get("http://localhost/v1/topics/urn:topic:1/resource-types"))
                .andExpect(status().isNotFound());
        verify(topicResourceTypeService, times(1)).getTopicResourceTypes(new URI("urn:topic:1"));
    }

    @Test
    public void testEmptyList() throws Exception {
        given(topicResourceTypeService.getTopicResourceTypes(new URI("urn:topic:1"))).willReturn(List.of());
        mockMvc.perform(get("http://localhost/v1/topics/urn:topic:1/resource-types"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]", true));
        verify(topicResourceTypeService, times(1)).getTopicResourceTypes(new URI("urn:topic:1"));
    }

    @Test
    public void testItemWithoutLanguageWithoutParent() throws Exception {
        Topic topic = new Topic();
        ResourceType resourceType = new ResourceType().name("Test");
        TopicResourceType topicResourceType = new TopicResourceType(topic, resourceType);
        given(topicResourceTypeService.getTopicResourceTypes(new URI("urn:topic:1"))).willReturn(List.of(topicResourceType));
        String json = "{" +
                "\"id\": \""+resourceType.getPublicId().toString()+"\"," +
                "\"name\": \"Test\"," +
                "\"parentId\": null," +
                "\"connectionId\": \""+topicResourceType.getPublicId().toString()+"\"" +
                "}";
        mockMvc.perform(get("http://localhost/v1/topics/urn:topic:1/resource-types"))
                .andExpect(status().isOk())
                .andExpect(content().json("["+json+"]", true));
    }


    @Test
    public void testItemWithoutLanguageWithParent() throws Exception {
        Topic topic = new Topic();
        ResourceType parentResourceType = new ResourceType();
        ResourceType resourceType = new ResourceType().name("Test");
        resourceType.setParent(parentResourceType);
        TopicResourceType topicResourceType = new TopicResourceType(topic, resourceType);
        given(topicResourceTypeService.getTopicResourceTypes(new URI("urn:topic:1"))).willReturn(List.of(topicResourceType));
        String json = "{" +
                "\"id\": \""+resourceType.getPublicId().toString()+"\"," +
                "\"name\": \"Test\"," +
                "\"parentId\": \""+parentResourceType.getPublicId().toString()+"\"," +
                "\"connectionId\": \""+topicResourceType.getPublicId().toString()+"\"" +
                "}";
        mockMvc.perform(get("http://localhost/v1/topics/urn:topic:1/resource-types"))
                .andExpect(status().isOk())
                .andExpect(content().json("["+json+"]", true));
    }

    @Test
    public void testItemWithoutTranslations() throws Exception {
        Topic topic = new Topic();
        ResourceType resourceType = spy(new ResourceType().name("Test"));
        TopicResourceType topicResourceType = new TopicResourceType(topic, resourceType);
        given(topicResourceTypeService.getTopicResourceTypes(new URI("urn:topic:1"))).willReturn(List.of(topicResourceType));
        String json = "{" +
                "\"id\": \""+resourceType.getPublicId().toString()+"\"," +
                "\"name\": \"Test\"," +
                "\"parentId\": null," +
                "\"connectionId\": \""+topicResourceType.getPublicId().toString()+"\"" +
                "}";
        mockMvc.perform(get("http://localhost/v1/topics/urn:topic:1/resource-types?language=en"))
                .andExpect(status().isOk())
                .andExpect(content().json("["+json+"]", true));
        verify(resourceType, times(1)).getTranslation("en");
    }

    @Test
    public void testItemWithoutMatchingTranslation() throws Exception {
        Topic topic = new Topic();
        ResourceType resourceType = new ResourceType().name("Test");
        resourceType.addTranslation("nb").setName("Test nb");
        TopicResourceType topicResourceType = new TopicResourceType(topic, resourceType);
        given(topicResourceTypeService.getTopicResourceTypes(new URI("urn:topic:1"))).willReturn(List.of(topicResourceType));
        String json = "{" +
                "\"id\": \""+resourceType.getPublicId().toString()+"\"," +
                "\"name\": \"Test\"," +
                "\"parentId\": null," +
                "\"connectionId\": \""+topicResourceType.getPublicId().toString()+"\"" +
                "}";
        mockMvc.perform(get("http://localhost/v1/topics/urn:topic:1/resource-types?language=en"))
                .andExpect(status().isOk())
                .andExpect(content().json("["+json+"]", true));
    }

    @Test
    public void testItemWithMatchingTranslation() throws Exception {
        Topic topic = new Topic();
        ResourceType resourceType = new ResourceType().name("Test");
        resourceType.addTranslation("nb").setName("Test nb");
        resourceType.addTranslation("en").setName("Test en");
        TopicResourceType topicResourceType = new TopicResourceType(topic, resourceType);
        given(topicResourceTypeService.getTopicResourceTypes(new URI("urn:topic:1"))).willReturn(List.of(topicResourceType));
        String json = "{" +
                "\"id\": \""+resourceType.getPublicId().toString()+"\"," +
                "\"name\": \"Test en\"," +
                "\"parentId\": null," +
                "\"connectionId\": \""+topicResourceType.getPublicId().toString()+"\"" +
                "}";
        mockMvc.perform(get("http://localhost/v1/topics/urn:topic:1/resource-types?language=en"))
                .andExpect(status().isOk())
                .andExpect(content().json("["+json+"]", true));
    }
}
