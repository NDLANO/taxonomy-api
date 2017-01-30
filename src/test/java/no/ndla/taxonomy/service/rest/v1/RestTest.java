package no.ndla.taxonomy.service.rest.v1;

import no.ndla.taxonomy.service.domain.Resource;
import no.ndla.taxonomy.service.domain.ResourceType;
import no.ndla.taxonomy.service.domain.Subject;
import no.ndla.taxonomy.service.domain.Topic;
import no.ndla.taxonomy.service.repositories.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringRunner;

import javax.persistence.EntityManager;

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
    TopicResourceRepository topicResourceRepository;

    @Autowired
    SubjectTopicRepository subjectTopicRepository;

    @Autowired
    TopicSubtopicRepository topicSubtopicRepository;

    @Autowired
    ResourceResourceTypeRepository resourceResourceTypeRepository;

    @Autowired
    ResourceRepository resourceRepository;

    @Autowired
    ResourceTypeRepository resourceTypeRepository;

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

    Resource newResource() {
        return save(new Resource());
    }

    ResourceType newResourceType() {
        return save(new ResourceType());
    }
}
