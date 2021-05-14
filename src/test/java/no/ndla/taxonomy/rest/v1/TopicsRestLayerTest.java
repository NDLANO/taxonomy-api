package no.ndla.taxonomy.rest.v1;

import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.repositories.TopicRepository;
import no.ndla.taxonomy.service.CachedUrlUpdaterService;
import no.ndla.taxonomy.service.TopicResourceTypeService;
import no.ndla.taxonomy.service.TopicService;
import no.ndla.taxonomy.service.dtos.TopicDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.net.URI;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = Topics.class)
public class TopicsRestLayerTest {
    @MockBean
    TopicRepository topicRepository;
    @MockBean
    TopicResourceTypeService topicResourceTypeService;
    @MockBean
    TopicService topicService;
    @MockBean
    CachedUrlUpdaterService cachedUrlUpdaterService;
    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testStartRecursiveCopy() throws Exception {
        Topic topic = new Topic();
        topic.setPublicId(URI.create("urn:topic:2"));
        topic.setName("Test");
        topic.setContentUri(URI.create("urn:article:1"));
        TopicDTO topicDto = new TopicDTO(topic, null);
        when(topicService.prepareRecursiveCopy(URI.create("urn:topic:1"))).thenReturn(topicDto);
        mockMvc.perform(MockMvcRequestBuilders.post(URI.create("http://localhost/v1/topics/urn:topic:1/copy")))
                .andExpect(status().isAccepted())
                .andExpect(content().json("{\"id\": \"urn:topic:2\"}"));
        Thread.sleep(1000);
        verify(topicService, times(1)).runRecursiveCopy(
                URI.create("urn:topic:1"), URI.create("urn:topic:2")
        );
    }
}
