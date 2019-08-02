package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Builder;
import no.ndla.taxonomy.repositories.TopicRepository;
import no.ndla.taxonomy.repositories.TopicSubtopicRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import static org.junit.Assert.assertEquals;

@SpringBootTest
@RunWith(SpringRunner.class)
public class TopicReusedInSameSubjectCloningServiceTest {
    @Autowired
    private TopicSubtopicRepository topicSubtopicRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private EntityManager entityManager;

    private Builder builder;

    @Before
    public void setUp() {
        builder = new Builder(entityManager);
    }

    /**
     * Limited test that basically only checks that the code runs without crashing
     */
    @Test
    @Transactional
    public void copyConflictingTopic() throws TopicReusedInSameSubjectCloningService.TopicIsNotInConflictException {
        final var service = new TopicReusedInSameSubjectCloningService(topicSubtopicRepository, topicRepository);

        final var subject1 = builder.subject(builder -> builder.publicId("urn:subject:1"));
        final var subject2 = builder.subject();

        final var rootTopic1 = builder.topic(builder -> builder.publicId("urn:topic:1"));
        final var rootTopic2 = builder.topic(builder -> builder.publicId("urn:topic:2"));

        subject1.addTopic(rootTopic1);
        subject1.addTopic(rootTopic2);

        final var subTopic1 = builder.topic(builder -> builder.publicId("urn:topic:subtopic1"));
        final var subTopic2 = builder.topic(builder -> builder.publicId("urn:topic:subtopic2"));

        rootTopic1.addSubtopic(subTopic1);
        rootTopic1.addSubtopic(subTopic2);
        rootTopic2.addSubtopic(subTopic2);

        subject2.addTopic(subTopic2);

        final var resource1 = builder.resource(builder -> builder.publicId("urn:resource:1"));
        final var resource2 = builder.resource(builder -> builder.publicId("urn:resource:2"));

        subTopic2.addResource(resource1);
        subTopic2.addResource(resource2);

        final var relevance = builder.relevance();
        final var filter = builder.filter();
        subTopic2.addFilter(filter, relevance);

        final var subTopicOfSubtopic2 = builder.topic(builder -> builder.publicId("urn:topic:subtopic2-2"));
        subTopic2.addSubtopic(subTopicOfSubtopic2);

        // subTopic2 exists two places in the same subject
        {
            final var result = service.copyConflictingTopic(subTopic2.getPublicId());

            assertEquals(1, result.getClonedTopics().size());
            final var clonedTopic = result.getClonedTopics().get(0).getClonedTopic();

            assertEquals(2, clonedTopic.getResources().size());
            assertEquals("urn:topic:1:subtopic2", clonedTopic.getPublicId().toString());

            assertEquals(1, clonedTopic.getTopicFilters().size());

            assertEquals(1, clonedTopic.getSubjectTopics().size());
        }
    }
}