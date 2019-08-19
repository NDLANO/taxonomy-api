package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Builder;
import no.ndla.taxonomy.domain.SubjectTopic;
import no.ndla.taxonomy.domain.TopicResource;
import no.ndla.taxonomy.domain.TopicSubtopic;
import no.ndla.taxonomy.repositories.SubjectTopicRepository;
import no.ndla.taxonomy.repositories.TopicResourceRepository;
import no.ndla.taxonomy.repositories.TopicSubtopicRepository;
import no.ndla.taxonomy.service.exceptions.DuplicateConnectionException;
import no.ndla.taxonomy.service.exceptions.InvalidArgumentServiceException;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.junit.Assert.*;

@SpringBootTest
@RunWith(SpringRunner.class)
@Transactional
public class EntityConnectionServiceImplTest {
    @Autowired
    private SubjectTopicRepository subjectTopicRepository;
    @Autowired
    private TopicSubtopicRepository topicSubtopicRepository;
    @Autowired
    private TopicResourceRepository topicResourceRepository;

    private EntityConnectionServiceImpl service;

    @Autowired
    private Builder builder;

    @Before
    public void setUp() throws Exception {
        service = new EntityConnectionServiceImpl(subjectTopicRepository, topicSubtopicRepository, topicResourceRepository);
    }

    @Test
    public void connectSubjectTopic() throws InvalidArgumentServiceException, DuplicateConnectionException {
        final var subject1 = builder.subject();
        final var subject2 = builder.subject();
        final var subject3 = builder.subject();

        final var topic1 = builder.topic();
        final var topic2 = builder.topic();
        final var topic3 = builder.topic();

        assertFalse(subjectTopicRepository.findFirstBySubjectAndTopic(subject1, topic1).isPresent());

        final var connection1 = service.connectSubjectTopic(subject1, topic1);
        assertNotNull(connection1);
        assertNotNull(connection1.getId());
        assertNotNull(connection1.getPublicId());
        assertTrue(connection1.isPrimary());
        assertEquals(1, connection1.getRank());

        assertTrue(subjectTopicRepository.findFirstBySubjectAndTopic(subject1, topic1).isPresent());

        final var connection2 = service.connectSubjectTopic(subject2, topic1);

        assertNotNull(connection2.getId());
        assertFalse(connection2.isPrimary());
        assertEquals(1, connection2.getRank());

        final var connection3 = service.connectSubjectTopic(subject3, topic1, true, 1);
        assertNotNull(connection3.getId());
        assertTrue(connection3.isPrimary());
        assertEquals(1, connection3.getRank());

        // There should be no difference in ranking on parent elements
        assertEquals(1, connection1.getRank());
        assertEquals(1, connection2.getRank());
        assertEquals(1, connection3.getRank());

        // Setting a new connection as primary must make old connections non-primary
        assertFalse(connection1.isPrimary());
        assertFalse(connection2.isPrimary());

        // If a primary topic-subtopic connection exists its should be set to non-primary if adding a primary subject-topic connection
        final var topicSubtopicConnection = TopicSubtopic.create(topic3, topic2, true);
        assertTrue(topicSubtopicConnection.isPrimary());
        final var connection4 = service.connectSubjectTopic(subject2, topic2, true, null);
        assertTrue(connection4.isPrimary());
        assertFalse(topicSubtopicConnection.isPrimary());
        assertEquals(1, connection4.getRank());

        // Testing ranking, ranking is ranking child object below parents
        final var subject4 = builder.subject();
        final var topic4 = builder.topic();
        final var topic5 = builder.topic();
        final var topic6 = builder.topic();
        final var topic7 = builder.topic();
        final var topic8 = builder.topic();

        final var connection5 = service.connectSubjectTopic(subject4, topic4);
        assertEquals(1, connection5.getRank());

        final var connection6 = service.connectSubjectTopic(subject4, topic5);
        assertEquals(1, connection5.getRank());
        assertEquals(2, connection6.getRank());

        final var connection7 = service.connectSubjectTopic(subject4, topic6, true, 2);
        assertEquals(1, connection5.getRank());
        assertEquals(3, connection6.getRank());
        assertEquals(2, connection7.getRank());

        final var connection8 = service.connectSubjectTopic(subject4, topic7, true, 1);
        assertEquals(2, connection5.getRank());
        assertEquals(4, connection6.getRank());
        assertEquals(3, connection7.getRank());
        assertEquals(1, connection8.getRank());

        final var connection9 = service.connectSubjectTopic(subject4, topic8, true, 5);
        assertEquals(2, connection5.getRank());
        assertEquals(4, connection6.getRank());
        assertEquals(3, connection7.getRank());
        assertEquals(1, connection8.getRank());
        assertEquals(5, connection9.getRank());

        try {
            service.connectSubjectTopic(subject4, topic5);
            fail("Expected DuplicateConnectionException");
        } catch (DuplicateConnectionException ignored) {

        }
    }

    @Test
    public void connectTopicSubtopic() throws InvalidArgumentServiceException, DuplicateConnectionException {
        final var topic1 = builder.topic();
        final var topic2 = builder.topic();
        final var topic3 = builder.topic();
        final var topic4 = builder.topic();
        final var topic5 = builder.topic();
        final var topic6 = builder.topic();
        final var topic7 = builder.topic();
        final var topic8 = builder.topic();
        final var topic9 = builder.topic();
        final var topic10 = builder.topic();
        final var topic11 = builder.topic();

        final var subject = builder.subject();

        // Add a primary subject-topic connection first to verify that creating a topic-subtopic connection will
        // be set to non-primary after adding a topic-subtopic connection

        final var subjectTopicConnection = SubjectTopic.create(subject, topic1, true);
        assertTrue(subjectTopicConnection.isPrimary());

        final var connection1 = service.connectTopicSubtopic(topic2, topic1, true, null);
        assertNotNull(connection1);
        assertNotNull(connection1.getId());
        assertTrue(connection1.isPrimary());
        assertFalse(subjectTopicConnection.isPrimary());
        assertEquals(1, connection1.getRank());
        assertSame(topic2, connection1.getTopic().orElse(null));
        assertSame(topic1, connection1.getSubtopic().orElse(null));

        final var connection2 = service.connectTopicSubtopic(topic3, topic1);
        assertFalse(connection2.isPrimary());
        assertEquals(1, connection2.getRank());
        assertSame(topic3, connection2.getTopic().orElse(null));
        assertSame(topic1, connection2.getSubtopic().orElse(null));

        final var connection3 = service.connectTopicSubtopic(topic4, topic1, true, null);
        assertTrue(connection3.isPrimary());
        assertEquals(1, connection3.getRank());
        assertFalse(connection1.isPrimary());
        assertFalse(connection2.isPrimary());
        assertSame(topic4, connection3.getTopic().orElse(null));
        assertSame(topic1, connection3.getSubtopic().orElse(null));

        // Test ranking
        final var connection4 = service.connectTopicSubtopic(topic4, topic5, true, null);
        assertEquals(1, connection3.getRank());
        assertEquals(2, connection4.getRank());

        final var connection5 = service.connectTopicSubtopic(topic4, topic6, true, null);
        assertEquals(1, connection3.getRank());
        assertEquals(2, connection4.getRank());
        assertEquals(3, connection5.getRank());

        final var connection6 = service.connectTopicSubtopic(topic4, topic7, true, 1);
        assertEquals(2, connection3.getRank());
        assertEquals(3, connection4.getRank());
        assertEquals(4, connection5.getRank());
        assertEquals(1, connection6.getRank());

        final var connection7 = service.connectTopicSubtopic(topic4, topic8, true, 3);
        assertEquals(2, connection3.getRank());
        assertEquals(4, connection4.getRank());
        assertEquals(5, connection5.getRank());
        assertEquals(1, connection6.getRank());
        assertEquals(3, connection7.getRank());

        final var connection8 = service.connectTopicSubtopic(topic4, topic9, true, 6);
        assertEquals(2, connection3.getRank());
        assertEquals(4, connection4.getRank());
        assertEquals(5, connection5.getRank());
        assertEquals(1, connection6.getRank());
        assertEquals(3, connection7.getRank());
        assertEquals(6, connection8.getRank());

        try {
            service.connectTopicSubtopic(topic4, topic8);
        } catch (DuplicateConnectionException ignored) {

        }

        // First parent topic connection will be primary regardless of request
        final var forcedPrimaryConnection1 = service.connectTopicSubtopic(topic4, topic10);
        assertTrue(forcedPrimaryConnection1.isPrimary());
        final var forcedPrimaryConnection2 = service.connectTopicSubtopic(topic4, topic11, false, null);
        assertTrue(forcedPrimaryConnection2.isPrimary());
    }

    @Test
    public void connectTopicResource() throws InvalidArgumentServiceException, DuplicateConnectionException {
        final var topic1 = builder.topic();
        final var topic2 = builder.topic();
        final var topic3 = builder.topic();
        final var topic4 = builder.topic();

        final var resource1 = builder.resource();
        final var resource2 = builder.resource();
        final var resource3 = builder.resource();
        final var resource4 = builder.resource();
        final var resource5 = builder.resource();
        final var resource6 = builder.resource();
        final var resource7 = builder.resource();

        final var connection1 = service.connectTopicResource(topic1, resource1, true, null);
        assertNotNull(connection1);
        assertSame(topic1, connection1.getTopic().orElse(null));
        assertSame(resource1, connection1.getResource().orElse(null));
        assertTrue(connection1.isPrimary());
        assertEquals(1, connection1.getRank());

        final var connection2 = service.connectTopicResource(topic1, resource2, true, null);
        assertNotNull(connection2);
        assertSame(topic1, connection2.getTopic().orElse(null));
        assertSame(resource2, connection2.getResource().orElse(null));
        assertTrue(connection2.isPrimary());
        assertEquals(2, connection2.getRank());

        assertTrue(connection1.isPrimary());
        assertEquals(1, connection1.getRank());

        final var connection3 = service.connectTopicResource(topic2, resource2, false, null);
        assertFalse(connection3.isPrimary());
        assertEquals(1, connection3.getRank());

        // Has not changed the first connection since setting this to non-primary
        assertTrue(connection2.isPrimary());

        // Test setting primary, should set old primary to non-primary
        final var connection4 = service.connectTopicResource(topic3, resource2, true, null);
        assertTrue(connection4.isPrimary());
        assertEquals(1, connection4.getRank());

        assertFalse(connection3.isPrimary());
        assertFalse(connection2.isPrimary());

        // Test ranking
        final var connection5 = service.connectTopicResource(topic4, resource1, true, null);
        assertEquals(1, connection5.getRank());

        final var connection6 = service.connectTopicResource(topic4, resource2);
        assertEquals(1, connection5.getRank());
        assertEquals(2, connection6.getRank());

        final var connection7 = service.connectTopicResource(topic4, resource3, true, 1);
        assertEquals(2, connection5.getRank());
        assertEquals(3, connection6.getRank());
        assertEquals(1, connection7.getRank());

        final var connection8 = service.connectTopicResource(topic4, resource4, true, 2);
        assertEquals(3, connection5.getRank());
        assertEquals(4, connection6.getRank());
        assertEquals(1, connection7.getRank());
        assertEquals(2, connection8.getRank());

        final var connection9 = service.connectTopicResource(topic4, resource5, true, 5);
        assertEquals(3, connection5.getRank());
        assertEquals(4, connection6.getRank());
        assertEquals(1, connection7.getRank());
        assertEquals(2, connection8.getRank());
        assertEquals(5, connection9.getRank());

        // First topic connection for a resource will be primary regardless of request
        final var forcedPrimaryConnection1 = service.connectTopicResource(topic4, resource6);
        assertTrue(forcedPrimaryConnection1.isPrimary());
        final var forcedPrimaryConnection2 = service.connectTopicResource(topic4, resource7, false, 1);
        assertTrue(forcedPrimaryConnection2.isPrimary());

        // Trying to add duplicate connection
        try {
            service.connectTopicResource(topic4, resource4);
            fail("Expected DuplicateConnectionException");
        } catch (DuplicateConnectionException ignored) {
        }
    }

    @Test
    public void disconnectTopicSubtopic() {
        final var topic1 = builder.topic();
        final var topic2 = builder.topic();
        final var subtopic1 = builder.topic();
        final var subtopic2 = builder.topic();
        final var subtopic3 = builder.topic();

        final var topic1subtopic1 = TopicSubtopic.create(topic1, subtopic1, true);
        final var topic1subtopic2 = TopicSubtopic.create(topic1, subtopic2, true);
        final var topic1subtopic3 = TopicSubtopic.create(topic1, subtopic3, true);

        final var topic2subtopic1 = TopicSubtopic.create(topic2, subtopic1, false);
        final var topic2subtopic2 = TopicSubtopic.create(topic2, subtopic2, false);
        final var topic2subtopic3 = TopicSubtopic.create(topic2, subtopic3, false);

        // Just verifies the pre-conditions of the created objects that is used for the test
        assertTrue(topic1subtopic1.isPrimary() && topic1subtopic2.isPrimary() && topic1subtopic3.isPrimary());
        assertFalse(topic2subtopic1.isPrimary() || topic2subtopic2.isPrimary() || topic2subtopic3.isPrimary());
        assertTrue(topic1.getChildrenTopicSubtopics().containsAll(Set.of(topic1subtopic1, topic1subtopic2, topic1subtopic3)));
        assertTrue(topic2.getChildrenTopicSubtopics().containsAll(Set.of(topic2subtopic1, topic2subtopic2, topic2subtopic3)));
        assertTrue(subtopic1.getParentTopicSubtopics().containsAll(Set.of(topic1subtopic1, topic2subtopic1)));
        assertTrue(subtopic2.getParentTopicSubtopics().containsAll(Set.of(topic1subtopic2, topic2subtopic2)));
        assertTrue(subtopic3.getParentTopicSubtopics().containsAll(Set.of(topic1subtopic3, topic2subtopic3)));
        assertEquals(3, topic1.getChildrenTopicSubtopics().size());
        assertEquals(3, topic2.getChildrenTopicSubtopics().size());
        assertEquals(2, subtopic1.getParentTopicSubtopics().size());
        assertEquals(2, subtopic2.getParentTopicSubtopics().size());
        assertEquals(2, subtopic3.getParentTopicSubtopics().size());

        service.disconnectTopicSubtopic(topic1subtopic1);

        assertFalse(topic1subtopic1.getTopic().isPresent());
        assertFalse(topic1subtopic1.getSubtopic().isPresent());
        assertFalse(topic1.getChildrenTopicSubtopics().contains(topic1subtopic1));
        assertEquals(2, topic1.getChildrenTopicSubtopics().size());
        assertEquals(3, topic2.getChildrenTopicSubtopics().size());
        assertEquals(1, subtopic1.getParentTopicSubtopics().size());
        assertEquals(2, subtopic2.getParentTopicSubtopics().size());
        assertEquals(2, subtopic3.getParentTopicSubtopics().size());
        assertTrue(topic2subtopic1.isPrimary());

        service.disconnectTopicSubtopic(topic2, subtopic1);

        assertFalse(topic2subtopic1.getTopic().isPresent());
        assertFalse(topic2subtopic1.getSubtopic().isPresent());
        assertFalse(topic2.getChildrenTopicSubtopics().contains(topic2subtopic1));
        assertEquals(2, topic1.getChildrenTopicSubtopics().size());
        assertEquals(2, topic2.getChildrenTopicSubtopics().size());
        assertEquals(0, subtopic1.getParentTopicSubtopics().size());
        assertEquals(2, subtopic2.getParentTopicSubtopics().size());
        assertEquals(2, subtopic3.getParentTopicSubtopics().size());

        service.disconnectTopicSubtopic(topic2, subtopic2);

        assertFalse(topic2subtopic2.getTopic().isPresent());
        assertFalse(topic2subtopic2.getSubtopic().isPresent());
        assertFalse(topic2.getChildrenTopicSubtopics().contains(topic2subtopic2));
        assertEquals(2, topic1.getChildrenTopicSubtopics().size());
        assertEquals(1, topic2.getChildrenTopicSubtopics().size());
        assertEquals(0, subtopic1.getParentTopicSubtopics().size());
        assertEquals(1, subtopic2.getParentTopicSubtopics().size());
        assertEquals(2, subtopic3.getParentTopicSubtopics().size());
        assertTrue(topic1subtopic2.isPrimary());

        service.disconnectTopicSubtopic(topic1, subtopic2);
        assertFalse(topic1subtopic2.getTopic().isPresent());
        assertFalse(topic1subtopic2.getSubtopic().isPresent());
        assertFalse(topic1.getChildrenTopicSubtopics().contains(topic1subtopic2));
        assertEquals(1, topic1.getChildrenTopicSubtopics().size());
        assertEquals(1, topic2.getChildrenTopicSubtopics().size());
        assertEquals(0, subtopic1.getParentTopicSubtopics().size());
        assertEquals(0, subtopic2.getParentTopicSubtopics().size());
        assertEquals(2, subtopic3.getParentTopicSubtopics().size());
        assertTrue(topic1subtopic2.isPrimary());

        // Test with multiple connections to subtopic and with SubjectTopic connections
        final var subtopic4 = builder.topic();
        final var topic3 = builder.topic();
        final var topic4 = builder.topic();
        final var subject1 = builder.subject();

        final var topic3subtopic4 = TopicSubtopic.create(topic3, subtopic4, true);
        final var topic4subtopic4 = TopicSubtopic.create(topic4, subtopic4, false);
        final var subject1subtopic4 = SubjectTopic.create(subject1, subtopic4, false);

        service.disconnectTopicSubtopic(topic3, subtopic4);
        assertTrue(topic4subtopic4.isPrimary() ^ subject1subtopic4.isPrimary());
        service.disconnectTopicSubtopic(topic4, subtopic4);
        assertTrue(subject1subtopic4.isPrimary());

        // Test with multiple levels
        final var subject2 = builder.subject();
        final var topic5 = builder.topic();
        final var topic6 = builder.topic();
        final var topic7 = builder.topic();
        final var topic8 = builder.topic();
        final var topic9 = builder.topic();

        final var subject2topic5 = SubjectTopic.create(subject2, topic5, true);
        final var topic5topic6 = TopicSubtopic.create(topic5, topic6, true);
        final var topic9topic6 = TopicSubtopic.create(topic9, topic6, false);
        final var topic5topic7 = TopicSubtopic.create(topic5, topic7, true);
        final var topic7topic8 = TopicSubtopic.create(topic7, topic8, true);

        assertTrue(subject2topic5.isPrimary());
        assertTrue(topic5topic6.isPrimary());
        assertFalse(topic9topic6.isPrimary());
        assertTrue(topic5topic7.isPrimary());
        assertTrue(topic7topic8.isPrimary());

        service.disconnectTopicSubtopic(topic5, topic6);

        assertTrue(subject2topic5.isPrimary());
        assertTrue(topic9topic6.isPrimary());
        assertTrue(topic5topic7.isPrimary());
        assertTrue(topic7topic8.isPrimary());

        service.disconnectTopicSubtopic(topic9, topic6);

        assertTrue(subject2topic5.isPrimary());
        assertTrue(topic5topic7.isPrimary());
        assertTrue(topic7topic8.isPrimary());

        service.disconnectTopicSubtopic(topic5, topic7);
        assertTrue(subject2topic5.isPrimary());
        assertTrue(topic7topic8.isPrimary());
    }

    @Test
    public void disconnectSubjectTopic() {
        final var subject1 = builder.subject();
        final var subject2 = builder.subject();
        final var subject3 = builder.subject();

        final var topic1 = builder.topic();
        final var topic2 = builder.topic();
        final var topic3 = builder.topic();
        final var topic4 = builder.topic();

        final var subject1topic1 = SubjectTopic.create(subject1, topic1, true);
        final var subject2topic1 = SubjectTopic.create(subject2, topic1, false);
        final var topic2topic1 = TopicSubtopic.create(topic2, topic1, false);
        final var subject2topic2 = SubjectTopic.create(subject2, topic2, true);
        final var subject2topic3 = SubjectTopic.create(subject2, topic3, true);
        final var subject2topic4 = SubjectTopic.create(subject2, topic4, true);

        assertTrue(subject1topic1.isPrimary());
        assertFalse(subject2topic1.isPrimary());
        assertFalse(topic2topic1.isPrimary());
        assertTrue(subject2topic2.isPrimary());
        assertTrue(subject2topic3.isPrimary());
        assertTrue(subject2topic4.isPrimary());

        assertTrue(subject1.getSubjectTopics().contains(subject1topic1));
        assertTrue(topic1.getSubjectTopics().contains(subject1topic1));

        service.disconnectSubjectTopic(subject1, topic1);

        assertFalse(subject1topic1.getTopic().isPresent());
        assertFalse(subject1topic1.getSubject().isPresent());

        assertFalse(subject1.getSubjectTopics().contains(subject1topic1));
        assertFalse(topic1.getSubjectTopics().contains(subject1topic1));

        assertTrue(subject2topic1.isPrimary() ^ topic2topic1.isPrimary());

        service.disconnectSubjectTopic(subject2topic1);

        assertTrue(topic2topic1.isPrimary());
    }

    @Test
    public void disconnectTopicResource() {
        final var topic1 = builder.topic();
        final var topic2 = builder.topic();
        final var topic3 = builder.topic();

        final var resource1 = builder.resource();
        final var resource2 = builder.resource();
        final var resource3 = builder.resource();

        final var topic1resource1 = TopicResource.create(topic1, resource1, true);
        final var topic1resource2 = TopicResource.create(topic1, resource2, true);
        final var topic1resource3 = TopicResource.create(topic1, resource3, true);

        final var topic2resource1 = TopicResource.create(topic2, resource1, false);
        final var topic3resource1 = TopicResource.create(topic3, resource1, false);

        assertTrue(topic1resource1.isPrimary());
        assertTrue(topic1resource2.isPrimary());
        assertTrue(topic1resource3.isPrimary());
        assertFalse(topic2resource1.isPrimary());
        assertFalse(topic3resource1.isPrimary());

        assertTrue(topic1.getTopicResources().contains(topic1resource1));
        assertTrue(resource1.getTopicResources().contains(topic1resource1));

        service.disconnectTopicResource(topic1, resource1);

        assertTrue(topic2resource1.isPrimary() ^ topic3resource1.isPrimary());
        assertFalse(topic1resource1.getResource().isPresent());
        assertFalse(topic1resource1.getTopic().isPresent());
        assertFalse(topic1.getTopicResources().contains(topic1resource1));
        assertFalse(resource1.getTopicResources().contains(topic1resource1));

        service.disconnectTopicResource(topic2resource1);
        assertTrue(topic3resource1.isPrimary());

        assertTrue(resource2.getTopicResources().contains(topic1resource2));
        assertTrue(resource3.getTopicResources().contains(topic1resource3));

        service.disconnectTopicResource(topic1resource2);
        service.disconnectTopicResource(topic1resource3);

        assertFalse(resource2.getTopicResources().contains(topic1resource2));
        assertFalse(resource3.getTopicResources().contains(topic1resource3));
    }

    @Test
    public void updateTopicSubtopic() throws NotFoundServiceException, InvalidArgumentServiceException {
        final var rootTopic1 = builder.topic();
        final var rootTopic2 = builder.topic();
        final var rootTopic3 = builder.topic();

        final var subTopic1 = builder.topic();
        final var subTopic2 = builder.topic();
        final var subTopic3 = builder.topic();

        final var subject1 = builder.subject();

        final var subjectTopic = SubjectTopic.create(subject1, subTopic2, true);

        final var connection1 = TopicSubtopic.create(rootTopic1, subTopic1, true);
        final var connection2 = TopicSubtopic.create(rootTopic1, subTopic2, false);
        final var connection3 = TopicSubtopic.create(rootTopic2, subTopic1, false);
        final var connection4 = TopicSubtopic.create(rootTopic2, subTopic2, false);

        final var connection5 = TopicSubtopic.create(rootTopic3, subTopic3, true);

        connection1.setRank(1);
        connection2.setRank(2);

        connection3.setRank(1);
        connection4.setRank(2);

        assertTrue(connection1.isPrimary());
        assertFalse(connection2.isPrimary());
        assertFalse(connection3.isPrimary());
        assertFalse(connection4.isPrimary());
        assertTrue(subjectTopic.isPrimary());

        assertEquals(1, connection1.getRank());
        assertEquals(2, connection2.getRank());
        assertEquals(1, connection3.getRank());
        assertEquals(2, connection4.getRank());

        service.updateTopicSubtopic(connection3, true, null);

        assertFalse(connection1.isPrimary());
        assertFalse(connection2.isPrimary());
        assertTrue(connection3.isPrimary());
        assertFalse(connection4.isPrimary());
        assertTrue(subjectTopic.isPrimary());

        assertEquals(1, connection1.getRank());
        assertEquals(2, connection2.getRank());
        assertEquals(1, connection3.getRank());
        assertEquals(2, connection4.getRank());

        service.updateTopicSubtopic(connection4, true, null);

        assertFalse(connection1.isPrimary());
        assertFalse(connection2.isPrimary());
        assertTrue(connection3.isPrimary());
        assertTrue(connection4.isPrimary());
        assertFalse(subjectTopic.isPrimary());

        service.updateTopicSubtopic(connection4, true, 1);

        assertEquals(1, connection1.getRank());
        assertEquals(2, connection2.getRank());
        assertEquals(2, connection3.getRank());
        assertEquals(1, connection4.getRank());

        service.updateTopicSubtopic(connection1, true, 2);
        assertEquals(2, connection1.getRank());
        assertEquals(3, connection2.getRank());
        assertEquals(2, connection3.getRank());
        assertEquals(1, connection4.getRank());

        assertTrue(connection1.isPrimary());
        assertFalse(connection2.isPrimary());
        assertFalse(connection3.isPrimary());
        assertTrue(connection4.isPrimary());
        assertFalse(subjectTopic.isPrimary());

        service.updateTopicSubtopic(connection1, false, 2);

        assertFalse(connection1.isPrimary());
        assertFalse(connection2.isPrimary());
        assertTrue(connection3.isPrimary());
        assertTrue(connection4.isPrimary());
        assertFalse(subjectTopic.isPrimary());

        try {
            // No other connection to set as primary
            service.updateTopicSubtopic(connection5, false, null);
            fail("Expected InvalidArgumentServiceException");
        } catch (InvalidArgumentServiceException ignored) {

        }
    }

    @Test
    public void updateSubjectTopic() throws NotFoundServiceException, InvalidArgumentServiceException {
        final var topic1 = builder.topic();
        final var topic2 = builder.topic();
        final var topic3 = builder.topic();

        final var subject1 = builder.subject();
        final var subject2 = builder.subject();
        final var subject3 = builder.subject();

        final var subject1topic1 = SubjectTopic.create(subject1, topic1, true);
        final var subject1topic2 = SubjectTopic.create(subject1, topic2, true);

        final var subject2topic1 = SubjectTopic.create(subject2, topic1, false);
        final var subject2topic2 = SubjectTopic.create(subject2, topic2, false);

        final var subject3topic3 = SubjectTopic.create(subject3, topic3, true);

        subject1topic1.setRank(1);
        subject1topic2.setRank(2);

        subject2topic1.setRank(1);
        subject2topic2.setRank(2);

        service.updateSubjectTopic(subject1topic1, false, null);
        assertFalse(subject1topic1.isPrimary());
        assertTrue(subject1topic2.isPrimary());
        assertTrue(subject2topic1.isPrimary());
        assertFalse(subject2topic2.isPrimary());

        service.updateSubjectTopic(subject2topic2, true, null);
        assertFalse(subject1topic1.isPrimary());
        assertFalse(subject1topic2.isPrimary());
        assertTrue(subject2topic1.isPrimary());
        assertTrue(subject2topic2.isPrimary());

        service.updateSubjectTopic(subject1topic2, false, 1);
        assertEquals(2, subject1topic1.getRank());
        assertEquals(1, subject1topic2.getRank());
        assertEquals(1, subject2topic1.getRank());
        assertEquals(2, subject2topic2.getRank());

        service.updateSubjectTopic(subject2topic2, false, 3);
        assertEquals(2, subject1topic1.getRank());
        assertEquals(1, subject1topic2.getRank());
        assertEquals(1, subject2topic1.getRank());
        assertEquals(3, subject2topic2.getRank());

        service.updateSubjectTopic(subject2topic2, false, 1);
        assertEquals(2, subject1topic1.getRank());
        assertEquals(1, subject1topic2.getRank());
        assertEquals(2, subject2topic1.getRank());
        assertEquals(1, subject2topic2.getRank());

        try {
            service.updateSubjectTopic(subject3topic3, false, null);
            fail("Expected InvalidArgumentServiceException");
        } catch (InvalidArgumentServiceException ignored) {

        }
        service.updateSubjectTopic(subject3topic3, true, null);

        final var subject4 = builder.subject();
        final var topic4 = builder.topic();
        final var topic5 = builder.topic();

        final var topic5topic4 = TopicSubtopic.create(topic5, topic4, true);
        final var subject4topic4 = SubjectTopic.create(subject4, topic4, false);

        assertTrue(topic5topic4.isPrimary());
        assertFalse(subject4topic4.isPrimary());

        service.updateSubjectTopic(subject4topic4, true, null);

        assertFalse(topic5topic4.isPrimary());
        assertTrue(subject4topic4.isPrimary());

    }

    @Test
    public void updateTopicResource() throws NotFoundServiceException, InvalidArgumentServiceException {
        final var topic1 = builder.topic();
        final var topic2 = builder.topic();

        final var resource1 = builder.resource();
        final var resource2 = builder.resource();
        final var resource3 = builder.resource();

        final var topic1resource1 = TopicResource.create(topic1, resource1, true);
        final var topic1resource2 = TopicResource.create(topic1, resource2, true);
        final var topic1resource3 = TopicResource.create(topic1, resource3, true);

        final var topic2resource1 = TopicResource.create(topic2, resource1, false);

        topic1resource1.setRank(1);
        topic1resource2.setRank(2);
        topic1resource3.setRank(3);

        assertTrue(topic1resource1.isPrimary());
        assertTrue(topic1resource2.isPrimary());
        assertTrue(topic1resource3.isPrimary());
        assertFalse(topic2resource1.isPrimary());

        service.updateTopicResource(topic2resource1, true, null);

        assertFalse(topic1resource1.isPrimary());
        assertTrue(topic1resource2.isPrimary());
        assertTrue(topic1resource3.isPrimary());
        assertTrue(topic2resource1.isPrimary());

        service.updateTopicResource(topic2resource1, false, null);

        assertTrue(topic1resource1.isPrimary());
        assertTrue(topic1resource2.isPrimary());
        assertTrue(topic1resource3.isPrimary());
        assertFalse(topic2resource1.isPrimary());

        try {
            service.updateTopicResource(topic1resource3, false, null);
            fail("Expected InvalidArgumentServiceException");
        } catch (InvalidArgumentServiceException ignored) {

        }

        assertEquals(1, topic1resource1.getRank());
        assertEquals(2, topic1resource2.getRank());
        assertEquals(3, topic1resource3.getRank());
        service.updateTopicResource(topic1resource3, true, 1);
        assertEquals(2, topic1resource1.getRank());
        assertEquals(3, topic1resource2.getRank());
        assertEquals(1, topic1resource3.getRank());

        service.updateTopicResource(topic1resource2, true, 2);
        assertEquals(3, topic1resource1.getRank());
        assertEquals(2, topic1resource2.getRank());
        assertEquals(1, topic1resource3.getRank());
    }

    @Test
    public void replacePrimaryConnectionsFor() {
        final var subject1 = builder.subject();
        final var subject2 = builder.subject();

        final var topic1 = builder.topic();
        final var topic2 = builder.topic();

        final var resource1 = builder.resource();

        final var subject1topic1 = SubjectTopic.create(subject1, topic1, true);
        final var subject2topic1 = SubjectTopic.create(subject2, topic1, false);

        assertTrue(subject1topic1.isPrimary());
        assertFalse(subject2topic1.isPrimary());

        service.replacePrimaryConnectionsFor(subject1);

        assertTrue(subject2topic1.isPrimary());
        assertFalse(subject1topic1.isPrimary());

        final var topic1resource1 = TopicResource.create(topic1, resource1, true);
        final var topic2resource1 = TopicResource.create(topic2, resource1, false);
        final var topic1topic2 = TopicSubtopic.create(topic1, topic2, true);
        final var subject2topic2 = SubjectTopic.create(subject2, topic2, false);

        assertTrue(topic1resource1.isPrimary());
        assertFalse(topic2resource1.isPrimary());
        assertTrue(topic1topic2.isPrimary());
        assertFalse(subject2topic2.isPrimary());

        service.replacePrimaryConnectionsFor(topic1);

        assertFalse(topic1resource1.isPrimary());
        assertTrue(topic2resource1.isPrimary());
        assertFalse(topic1topic2.isPrimary());
        assertTrue(subject2topic2.isPrimary());

    }
}