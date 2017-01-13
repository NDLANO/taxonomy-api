package no.ndla.taxonomy.service.rest.v1;

import no.ndla.taxonomy.service.domain.Subject;
import no.ndla.taxonomy.service.domain.Topic;
import no.ndla.taxonomy.service.repositories.SubjectRepository;
import no.ndla.taxonomy.service.repositories.SubjectTopicRepository;
import no.ndla.taxonomy.service.repositories.TopicRepository;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringRunner;

import javax.persistence.EntityManager;

import static no.ndla.taxonomy.service.TestUtils.clearGraph;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("junit")
public abstract class RestTest extends AbstractTransactionalJUnit4SpringContextTests {

    @Autowired
    EntityManager entityManager;

    @Autowired
    SubjectRepository subjectRepository;

    @Autowired
    TopicRepository topicRepository;

    @Autowired
    SubjectTopicRepository subjectTopicRepository;

    @Before
    public void setup() throws Exception {
        clearGraph();
    }


    <T> T save(T entity) {
        entityManager.persist(entity);
        return entity;
    }

    Subject newSubject() {
        return save(new Subject());
    }

    Topic newTopic() {
        return save(new Topic());
    }
}
