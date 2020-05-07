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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@Transactional
@DirtiesContext
public class EntityConnectionServiceImplTest {
    @Autowired
    private SubjectTopicRepository subjectTopicRepository;
    @Autowired
    private TopicSubtopicRepository topicSubtopicRepository;
    @Autowired
    private TopicResourceRepository topicResourceRepository;

    private CachedUrlUpdaterService cachedUrlUpdaterService;

    private EntityConnectionServiceImpl service;

    @Autowired
    private Builder builder;

    @BeforeEach
    public void setUp() throws Exception {
        cachedUrlUpdaterService = mock(CachedUrlUpdaterService.class);

        service = new EntityConnectionServiceImpl(subjectTopicRepository, topicSubtopicRepository, topicResourceRepository, cachedUrlUpdaterService);
    }

    @Test
    public void connectSubjectTopic() {
        final var subject1 = builder.subject();
        final var subject2 = builder.subject();

        final var topic1 = builder.topic();
        final var topic2 = builder.topic();

        assertFalse(subjectTopicRepository.findFirstBySubjectAndTopic(subject1, topic1).isPresent());

        final var connection1 = service.connectSubjectTopic(subject1, topic1);
        assertNotNull(connection1);
        assertNotNull(connection1.getId());
        assertNotNull(connection1.getPublicId());
        assertEquals(1, connection1.getRank());

        verify(cachedUrlUpdaterService, atLeastOnce()).updateCachedUrls(topic1);

        assertTrue(subjectTopicRepository.findFirstBySubjectAndTopic(subject1, topic1).isPresent());

        final var connection2 = service.connectSubjectTopic(subject2, topic2);

        assertNotNull(connection2.getId());
        assertEquals(1, connection2.getRank());

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

        final var connection7 = service.connectSubjectTopic(subject4, topic6, 2);
        assertEquals(1, connection5.getRank());
        assertEquals(3, connection6.getRank());
        assertEquals(2, connection7.getRank());

        final var connection8 = service.connectSubjectTopic(subject4, topic7, 1);
        assertEquals(2, connection5.getRank());
        assertEquals(4, connection6.getRank());
        assertEquals(3, connection7.getRank());
        assertEquals(1, connection8.getRank());

        final var connection9 = service.connectSubjectTopic(subject4, topic8, 5);
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
    public void connectTopicSubtopic() {
        final var topic1 = builder.topic();
        final var topic2 = builder.topic();
        final var topic4 = builder.topic();
        final var topic5 = builder.topic();
        final var topic6 = builder.topic();
        final var topic7 = builder.topic();
        final var topic8 = builder.topic();
        final var topic9 = builder.topic();

        final var connection1 = service.connectTopicSubtopic(topic2, topic1, null);
        assertNotNull(connection1);
        assertNotNull(connection1.getId());
        assertEquals(1, connection1.getRank());
        assertSame(topic2, connection1.getTopic().orElse(null));
        assertSame(topic1, connection1.getSubtopic().orElse(null));

        verify(cachedUrlUpdaterService, atLeastOnce()).updateCachedUrls(topic1);

        // Test ranking
        final var connection4 = service.connectTopicSubtopic(topic4, topic5, null);
        assertEquals(1, connection4.getRank());

        final var connection5 = service.connectTopicSubtopic(topic4, topic6, null);
        assertEquals(1, connection4.getRank());
        assertEquals(2, connection5.getRank());

        final var connection6 = service.connectTopicSubtopic(topic4, topic7, 1);
        assertEquals(2, connection4.getRank());
        assertEquals(3, connection5.getRank());
        assertEquals(1, connection6.getRank());

        final var connection7 = service.connectTopicSubtopic(topic4, topic8, 3);
        assertEquals(2, connection4.getRank());
        assertEquals(4, connection5.getRank());
        assertEquals(1, connection6.getRank());
        assertEquals(3, connection7.getRank());

        final var connection8 = service.connectTopicSubtopic(topic4, topic9, 5);
        assertEquals(2, connection4.getRank());
        assertEquals(4, connection5.getRank());
        assertEquals(1, connection6.getRank());
        assertEquals(3, connection7.getRank());
        assertEquals(5, connection8.getRank());

        try {
            service.connectTopicSubtopic(topic4, topic8);
        } catch (DuplicateConnectionException ignored) {

        }

        // try to create loops
        final var topic10 = builder.topic();
        final var topic11 = builder.topic();
        final var topic12 = builder.topic();

        try {
            service.connectTopicSubtopic(topic10, topic10);
            fail("Expected InvalidArgumentServiceException");
        } catch (InvalidArgumentServiceException ignored) {

        }

        service.connectTopicSubtopic(topic10, topic11);
        service.connectTopicSubtopic(topic11, topic12);

        try {
            service.connectTopicSubtopic(topic12, topic10);
            fail("Expected InvalidArgumentServiceException");
        } catch (InvalidArgumentServiceException ignored) {

        }

        try {
            service.connectTopicSubtopic(topic11, topic10);
            fail("Expected InvalidArgumentServiceException");
        } catch (InvalidArgumentServiceException ignored) {

        }

        try {
            service.connectTopicSubtopic(topic12, topic11);
            fail("Expected DuplicateConnectionException");
        } catch (DuplicateConnectionException ignored) {

        }

        try {
            service.connectTopicSubtopic(topic11, topic10);
            fail("Expected InvalidArgumentServiceException");
        } catch (InvalidArgumentServiceException ignored) {

        }
    }

    @Test
    public void connectTopicResource() {
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
        assertTrue(connection1.isPrimary().orElseThrow());
        assertEquals(1, connection1.getRank());

        verify(cachedUrlUpdaterService, atLeastOnce()).updateCachedUrls(resource1);

        final var connection2 = service.connectTopicResource(topic1, resource2, true, null);
        assertNotNull(connection2);
        assertSame(topic1, connection2.getTopic().orElse(null));
        assertSame(resource2, connection2.getResource().orElse(null));
        assertTrue(connection2.isPrimary().orElseThrow());
        assertEquals(2, connection2.getRank());

        assertTrue(connection1.isPrimary().orElseThrow());
        assertEquals(1, connection1.getRank());

        final var connection3 = service.connectTopicResource(topic2, resource2, false, null);
        assertFalse(connection3.isPrimary().orElseThrow());
        assertEquals(1, connection3.getRank());

        // Has not changed the first connection since setting this to non-primary
        assertTrue(connection2.isPrimary().orElseThrow());

        // Test setting primary, should set old primary to non-primary
        final var connection4 = service.connectTopicResource(topic3, resource2, true, null);
        assertTrue(connection4.isPrimary().orElseThrow());
        assertEquals(1, connection4.getRank());

        assertFalse(connection3.isPrimary().orElseThrow());
        assertFalse(connection2.isPrimary().orElseThrow());

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
        assertTrue(forcedPrimaryConnection1.isPrimary().orElseThrow());
        final var forcedPrimaryConnection2 = service.connectTopicResource(topic4, resource7, false, 1);
        assertTrue(forcedPrimaryConnection2.isPrimary().orElseThrow());

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
        final var subtopic1 = builder.topic();
        final var subtopic2 = builder.topic();
        final var subtopic3 = builder.topic();

        final var topic1subtopic1 = TopicSubtopic.create(topic1, subtopic1);
        final var topic1subtopic2 = TopicSubtopic.create(topic1, subtopic2);
        final var topic1subtopic3 = TopicSubtopic.create(topic1, subtopic3);

        // Just verifies the pre-conditions of the created objects that is used for the test
        assertTrue(topic1.getChildrenTopicSubtopics().containsAll(Set.of(topic1subtopic1, topic1subtopic2, topic1subtopic3)));
        assertSame(topic1subtopic1, subtopic1.getParentTopicSubtopic().orElseThrow());
        assertSame(topic1subtopic2, subtopic2.getParentTopicSubtopic().orElseThrow());
        assertSame(topic1subtopic3, subtopic3.getParentTopicSubtopic().orElseThrow());
        assertEquals(3, topic1.getChildrenTopicSubtopics().size());

        reset(cachedUrlUpdaterService);

        service.disconnectTopicSubtopic(topic1subtopic1);

        verify(cachedUrlUpdaterService).updateCachedUrls(subtopic1);

        assertFalse(topic1subtopic1.getTopic().isPresent());
        assertFalse(topic1subtopic1.getSubtopic().isPresent());
        assertFalse(topic1.getChildrenTopicSubtopics().contains(topic1subtopic1));
        assertEquals(2, topic1.getChildrenTopicSubtopics().size());
        assertFalse(subtopic1.getParentTopicSubtopic().isPresent());
        assertSame(topic1subtopic2, subtopic2.getParentTopicSubtopic().orElseThrow());
        assertSame(topic1subtopic3, subtopic3.getParentTopicSubtopic().orElseThrow());


        service.disconnectTopicSubtopic(topic1, subtopic2);
        assertFalse(topic1subtopic2.getTopic().isPresent());
        assertFalse(topic1subtopic2.getSubtopic().isPresent());
        assertFalse(topic1.getChildrenTopicSubtopics().contains(topic1subtopic2));
        assertEquals(1, topic1.getChildrenTopicSubtopics().size());
        assertFalse(subtopic1.getParentTopicSubtopic().isPresent());
        assertFalse(subtopic2.getParentTopicSubtopic().isPresent());
        assertSame(topic1subtopic3, subtopic3.getParentTopicSubtopic().orElseThrow());
    }

    @Test
    public void disconnectSubjectTopic() {
        final var subject1 = builder.subject();

        final var topic1 = builder.topic();

        final var subject1topic1 = SubjectTopic.create(subject1, topic1);

        assertTrue(subject1.getSubjectTopics().contains(subject1topic1));
        assertTrue(topic1.getSubjectTopics().contains(subject1topic1));

        reset(cachedUrlUpdaterService);

        service.disconnectSubjectTopic(subject1, topic1);

        verify(cachedUrlUpdaterService).updateCachedUrls(topic1);

        assertFalse(subject1topic1.getTopic().isPresent());
        assertFalse(subject1topic1.getSubject().isPresent());

        assertFalse(subject1.getSubjectTopics().contains(subject1topic1));
        assertFalse(topic1.getSubjectTopics().contains(subject1topic1));
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

        assertTrue(topic1resource1.isPrimary().orElseThrow());
        assertTrue(topic1resource2.isPrimary().orElseThrow());
        assertTrue(topic1resource3.isPrimary().orElseThrow());
        assertFalse(topic2resource1.isPrimary().orElseThrow());
        assertFalse(topic3resource1.isPrimary().orElseThrow());

        assertTrue(topic1.getTopicResources().contains(topic1resource1));
        assertTrue(resource1.getTopicResources().contains(topic1resource1));

        reset(cachedUrlUpdaterService);

        service.disconnectTopicResource(topic1, resource1);

        verify(cachedUrlUpdaterService, atLeastOnce()).updateCachedUrls(resource1);

        assertTrue(topic2resource1.isPrimary().orElseThrow() ^ topic3resource1.isPrimary().orElseThrow());
        assertFalse(topic1resource1.getResource().isPresent());
        assertFalse(topic1resource1.getTopic().isPresent());
        assertFalse(topic1.getTopicResources().contains(topic1resource1));
        assertFalse(resource1.getTopicResources().contains(topic1resource1));

        service.disconnectTopicResource(topic2resource1);
        assertTrue(topic3resource1.isPrimary().orElseThrow());

        assertTrue(resource2.getTopicResources().contains(topic1resource2));
        assertTrue(resource3.getTopicResources().contains(topic1resource3));

        service.disconnectTopicResource(topic1resource2);
        service.disconnectTopicResource(topic1resource3);

        assertFalse(resource2.getTopicResources().contains(topic1resource2));
        assertFalse(resource3.getTopicResources().contains(topic1resource3));
    }

    @Test
    public void updateTopicSubtopic() {
        final var rootTopic1 = builder.topic();
        final var rootTopic3 = builder.topic();

        final var subTopic1 = builder.topic();
        final var subTopic2 = builder.topic();
        final var subTopic3 = builder.topic();

        final var subject1 = builder.subject();

        final var subjectTopic = SubjectTopic.create(subject1, subTopic2);

        final var connection1 = TopicSubtopic.create(rootTopic1, subTopic1);

        final var connection5 = TopicSubtopic.create(rootTopic3, subTopic3);

        connection1.setRank(1);

        assertEquals(1, connection1.getRank());

        service.updateTopicSubtopic(connection1, 2);
        assertEquals(2, connection1.getRank());
    }

    @Test
    public void updateSubjectTopic() {
        final var topic1 = builder.topic();
        final var topic2 = builder.topic();
        final var topic3 = builder.topic();

        final var subject1 = builder.subject();
        final var subject3 = builder.subject();

        final var subject1topic1 = SubjectTopic.create(subject1, topic1);
        final var subject1topic2 = SubjectTopic.create(subject1, topic2);

        SubjectTopic.create(subject3, topic3);

        subject1topic1.setRank(1);
        subject1topic2.setRank(2);

        service.updateSubjectTopic(subject1topic2, 1);
        assertEquals(2, subject1topic1.getRank());
        assertEquals(1, subject1topic2.getRank());

    }

    @Test
    public void updateTopicResource() {
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

        assertTrue(topic1resource1.isPrimary().orElseThrow());
        assertTrue(topic1resource2.isPrimary().orElseThrow());
        assertTrue(topic1resource3.isPrimary().orElseThrow());
        assertFalse(topic2resource1.isPrimary().orElseThrow());

        service.updateTopicResource(topic2resource1, true, null);

        assertFalse(topic1resource1.isPrimary().orElseThrow());
        assertTrue(topic1resource2.isPrimary().orElseThrow());
        assertTrue(topic1resource3.isPrimary().orElseThrow());
        assertTrue(topic2resource1.isPrimary().orElseThrow());

        service.updateTopicResource(topic2resource1, false, null);

        assertTrue(topic1resource1.isPrimary().orElseThrow());
        assertTrue(topic1resource2.isPrimary().orElseThrow());
        assertTrue(topic1resource3.isPrimary().orElseThrow());
        assertFalse(topic2resource1.isPrimary().orElseThrow());

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

        SubjectTopic.create(subject1, topic1);

        final var topic1resource1 = TopicResource.create(topic1, resource1, true);
        final var topic2resource1 = TopicResource.create(topic2, resource1, false);
        final var topic1topic2 = TopicSubtopic.create(topic1, topic2);

        assertTrue(topic1resource1.isPrimary().orElseThrow());
        assertFalse(topic2resource1.isPrimary().orElseThrow());

        service.replacePrimaryConnectionsFor(topic1);

        assertFalse(topic1resource1.isPrimary().orElseThrow());
        assertTrue(topic2resource1.isPrimary().orElseThrow());
    }
}