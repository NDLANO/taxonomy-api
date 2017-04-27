package no.ndla.taxonomy.service.rest.v1;

import no.ndla.taxonomy.service.domain.*;
import no.ndla.taxonomy.service.repositories.*;
import org.junit.Before;
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

    @Autowired
    CachedUrlRepository cachedUrlRepository;

    @Autowired
    FilterRepository filterRepository;

    @Autowired
    RelevanceRepository relevanceRepository;

    @Autowired
    ResourceFilterRepository resourceFilterRepository;

    @Autowired
    TopicFilterRepository topicFilterRepository;

    Builder builder;

    @Before
    public void setUp() throws Exception {
        builder = new Builder(entityManager);
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

    Resource newResource() {
        return save(new Resource());
    }

    ResourceType newResourceType() {
        return save(new ResourceType());
    }
}
