package no.ndla.taxonomy.service.rest.v1;


import no.ndla.taxonomy.service.domain.Subject;
import no.ndla.taxonomy.service.domain.SubjectTopic;
import no.ndla.taxonomy.service.domain.Topic;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.URI;

import static no.ndla.taxonomy.service.TestUtils.*;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SubjectTopicsTest extends RestTest {

    @Test
    public void can_add_topic_to_subject() throws Exception {
        URI subjectId, topicId;
        subjectId = newSubject().name("physics").getPublicId();
        topicId = newTopic().name("trigonometry").getPublicId();

        URI id = getId(
                createResource("/v1/subject-topics", new SubjectTopics.AddTopicToSubjectCommand() {{
                    this.subjectid = subjectId;
                    this.topicid = topicId;
                }})
        );

        Subject physics = subjectRepository.getByPublicId(subjectId);
        assertEquals(1, count(physics.getTopics()));
        assertAnyTrue(physics.getTopics(), t -> "trigonometry".equals(t.getName()));
        assertNotNull(subjectTopicRepository.getByPublicId(id));
    }

    @Test
    public void canot_add_existing_topic_to_subject() throws Exception {
        Subject physics = newSubject().name("physics");
        Topic trigonometry = newTopic().name("trigonometry");
        physics.addTopic(trigonometry);

        URI subjectId = physics.getPublicId();
        URI topicId = trigonometry.getPublicId();

        createResource("/v1/subject-topics", new SubjectTopics.AddTopicToSubjectCommand() {{
                    this.subjectid = subjectId;
                    this.topicid = topicId;
                }},
                status().isConflict()
        );
    }

    @Test
    public void can_delete_subject_topic() throws Exception {
        URI id = save(newSubject().addTopic(newTopic())).getPublicId();
        deleteResource("/v1/subject-topics/" + id);
        assertNull(subjectRepository.findByPublicId(id));
    }

    @Test
    public void can_update_subject_topic() throws Exception {
        URI id = save(newSubject().addTopic(newTopic())).getPublicId();

        updateResource("/v1/subject-topics/" + id, new SubjectTopics.UpdateSubjectTopicCommand() {{
            primary = true;
        }});

        assertTrue(subjectTopicRepository.getByPublicId(id).isPrimary());
    }

    @Test
    public void can_get_topics() throws Exception {
        Subject physics = newSubject().name("physics");
        Topic electricity = newTopic().name("electricity");
        save(physics.addTopic(electricity));

        Subject mathematics = newSubject().name("mathematics");
        Topic trigonometry = newTopic().name("trigonometry");
        save(mathematics.addTopic(trigonometry));

        URI physicsId = physics.getPublicId();
        URI electricityId = electricity.getPublicId();
        URI mathematicsId = mathematics.getPublicId();
        URI trigonometryId = trigonometry.getPublicId();

        MockHttpServletResponse response = getResource("/v1/subject-topics");
        SubjectTopics.SubjectTopicIndexDocument[] subjectTopics = getObject(SubjectTopics.SubjectTopicIndexDocument[].class, response);

        assertEquals(2, subjectTopics.length);
        assertAnyTrue(subjectTopics, t -> physicsId.equals(t.subjectid) && electricityId.equals(t.topicid));
        assertAnyTrue(subjectTopics, t -> mathematicsId.equals(t.subjectid) && trigonometryId.equals(t.topicid));
        assertAllTrue(subjectTopics, t -> isValidId(t.id));
    }

    @Test
    public void can_get_subject_topic() throws Exception {
        Subject physics = newSubject().name("physics");
        Topic electricity = newTopic().name("electricity");
        SubjectTopic subjectTopic = save(physics.addTopic(electricity));

        URI subjectid = physics.getPublicId();
        URI topicid = electricity.getPublicId();
        URI id = subjectTopic.getPublicId();

        MockHttpServletResponse resource = getResource("/v1/subject-topics/" + id);
        SubjectTopics.SubjectTopicIndexDocument subjectTopicIndexDocument = getObject(SubjectTopics.SubjectTopicIndexDocument.class, resource);
        assertEquals(subjectid, subjectTopicIndexDocument.subjectid);
        assertEquals(topicid, subjectTopicIndexDocument.topicid);
    }
}
