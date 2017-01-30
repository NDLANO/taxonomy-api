package no.ndla.taxonomy.service.rest.v1;


import no.ndla.taxonomy.service.domain.Resource;
import no.ndla.taxonomy.service.domain.ResourceType;
import no.ndla.taxonomy.service.domain.Topic;
import no.ndla.taxonomy.service.repositories.TopicRepository;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;

import static no.ndla.taxonomy.service.TestUtils.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TopicsTest extends RestTest {

    @Autowired
    private TopicRepository topicRepository;

    @Test
    public void can_get_all_topics() throws Exception {
        newTopic().name("photo synthesis");
        newTopic().name("trigonometry");

        MockHttpServletResponse response = getResource("/v1/topics");
        Topics.TopicIndexDocument[] topics = getObject(Topics.TopicIndexDocument[].class, response);
        assertEquals(2, topics.length);

        assertAnyTrue(topics, s -> "photo synthesis".equals(s.name));
        assertAnyTrue(topics, s -> "trigonometry".equals(s.name));
        assertAllTrue(topics, s -> isValidId(s.id));
    }

    @Test
    public void can_create_topic() throws Exception {
        Topics.CreateTopicCommand createTopicCommand = new Topics.CreateTopicCommand() {{
            name = "trigonometry";
        }};

        MockHttpServletResponse response = createResource("/v1/topics", createTopicCommand);
        URI id = getId(response);

        Topic topic = topicRepository.getByPublicId(id);
        assertEquals(createTopicCommand.name, topic.getName());
    }

    @Test
    public void can_create_topic_with_id() throws Exception {
        Topics.CreateTopicCommand createTopicCommand = new Topics.CreateTopicCommand() {{
            id = URI.create("urn:topic:1");
            name = "trigonometry";
        }};

        createResource("/v1/topics", createTopicCommand);

        Topic topic = topicRepository.getByPublicId(createTopicCommand.id);
        assertEquals(createTopicCommand.name, topic.getName());
    }

    @Test
    public void duplicate_ids_not_allowed() throws Exception {
        Topics.CreateTopicCommand command = new Topics.CreateTopicCommand() {{
            id = URI.create("urn:topic:1");
            name = "name";
        }};

        createResource("/v1/topics", command, status().isCreated());
        createResource("/v1/topics", command, status().isConflict());
    }

    @Test
    public void can_update_topic() throws Exception {
        URI id = newTopic().getPublicId();

        Topics.UpdateTopicCommand command = new Topics.UpdateTopicCommand() {{
            name = "trigonometry";
        }};

        updateResource("/v1/topics/" + id, command);

        Topic topic = topicRepository.getByPublicId(id);
        assertEquals(command.name, topic.getName());
    }

    @Test
    public void can_delete_topic() throws Exception {
        URI id = newTopic().getPublicId();
        deleteResource("/v1/topics/" + id);
        assertNull(topicRepository.findByPublicId(id));
    }

    @Test
    public void can_get_resources_for_a_topic_recursively() throws Exception {
        Topic a = newTopic().name("a");
        Topic aa = newTopic().name("aa");
        Topic aaa = newTopic().name("aaa");
        Topic aab = newTopic().name("aab");
        save(a.addSubtopic(aa));
        save(aa.addSubtopic(aaa));
        save(aa.addSubtopic(aab));

        Resource resourceA = newResource().name("resource a");
        save(a.addResource(resourceA));
        Resource resourceAA = newResource().name("resource aa");
        save(aa.addResource(resourceAA));
        Resource resourceAAA = newResource().name("resource aaa");
        save(aaa.addResource(resourceAAA));
        Resource resourceAAB = newResource().name("resource aab");
        save(aab.addResource(resourceAAB));

        flush();

        MockHttpServletResponse response = getResource("/v1/topics/" + a.getPublicId().toString() + "/resources?recursive=true");
        Topics.ResourceIndexDocument[] result = getObject(Topics.ResourceIndexDocument[].class, response);

        assertEquals(4, result.length);
        assertAnyTrue(result, r -> resourceA.getName().equals(r.name));
        assertAnyTrue(result, r -> resourceAA.getName().equals(r.name));
        assertAnyTrue(result, r -> resourceAAA.getName().equals(r.name));
        assertAnyTrue(result, r -> resourceAAB.getName().equals(r.name));
    }

    @Test
    public void can_get_resources_for_a_topic_without_child_topic_resources() throws Exception {
        Topic a = newTopic().name("a");
        Topic aa = newTopic().name("aa");
        save(a.addSubtopic(aa));
        Resource resourceA = newResource().name("resource a");
        save(a.addResource(resourceA));
        Resource resourceAA = newResource().name("resource aa");
        save(aa.addResource(resourceAA));
        Resource resourceA2 = newResource().name("resource a2");
        save(a.addResource(resourceA2));
        flush();

        MockHttpServletResponse response = getResource("/v1/topics/" + a.getPublicId().toString() + "/resources");
        Topics.ResourceIndexDocument[] result = getObject(Topics.ResourceIndexDocument[].class, response);

        assertEquals(2, result.length);
        assertAnyTrue(result, r -> resourceA.getName().equals((r.name)));
        assertAnyTrue(result, r -> resourceA2.getName().equals(r.name));
    }

    @Test
    @Ignore
    public void can_get_resources_for_a_topic_filtered_on_resource_type() throws Exception {
        Topic a = newTopic().name("a");
        Topic aa = newTopic().name("aa");
        save(a.addSubtopic(aa));
        Resource resourceA = newResource().name("resource a");
        save(a.addResource(resourceA));
        Resource resourceAA = newResource().name("resource aa");
        save(aa.addResource(resourceAA));
        Resource resourceA2 = newResource().name("resource a2");
        save(a.addResource(resourceA2));
        ResourceType type1 = newResourceType().name("lecture");
        resourceA.addResourceType(type1);
        ResourceType type2 = newResourceType().name("test");
        resourceA2.addResourceType(type2);
        flush();

        MockHttpServletResponse response = getResource("/v1/topics/" + a.getPublicId().toString() + "/resources?type=" + type1.getPublicId());
        Topics.ResourceIndexDocument[] result = getObject(Topics.ResourceIndexDocument[].class, response);

        assertEquals(1, result.length);
        assertAnyTrue(result, r -> resourceA.getName().equals(r.name));

    }
}
