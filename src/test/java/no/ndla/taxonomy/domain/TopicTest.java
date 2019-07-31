package no.ndla.taxonomy.domain;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

public class TopicTest {
    private Topic topic;

    @Before
    public void setUp() {
        topic = new Topic();
    }

    @Test
    public void name() {
        assertEquals(topic, topic.name("test name 1"));
        assertEquals("test name 1", topic.getName());
    }

    @Test
    public void getPrimaryPath() {
        assertFalse(topic.getPrimaryPath().isPresent());

        final var cachedNonPrimaryContextUrl = mock(CachedUrl.class);
        final var cachedNonPrimarySubjectUrl = mock(CachedUrl.class);
        final var cachedPrimaryContextUrl = mock(CachedUrl.class);
        final var cachedPrimarySubjectUrl = mock(CachedUrl.class);

        when(cachedNonPrimaryContextUrl.isPrimary()).thenReturn(false);
        when(cachedNonPrimarySubjectUrl.isPrimary()).thenReturn(false);
        when(cachedPrimaryContextUrl.isPrimary()).thenReturn(true);
        when(cachedPrimarySubjectUrl.isPrimary()).thenReturn(true);

        when(cachedNonPrimaryContextUrl.getPath()).thenReturn("/topic/non-primary");
        when(cachedNonPrimarySubjectUrl.getPath()).thenReturn("/subject/non-primary");
        when(cachedPrimaryContextUrl.getPath()).thenReturn("/topic/primary");
        when(cachedPrimarySubjectUrl.getPath()).thenReturn("/subject/primary");

        // If just returning non-primary paths getPrimaryPath() must return none
        setField(topic, "cachedUrls", Set.of(cachedNonPrimaryContextUrl, cachedNonPrimarySubjectUrl));
        assertFalse(topic.getPrimaryPath().isPresent());

        // If returning a primary subject path along it must return it
        setField(topic, "cachedUrls", Set.of(cachedNonPrimaryContextUrl, cachedNonPrimarySubjectUrl, cachedPrimarySubjectUrl));
        assertEquals("/subject/primary", topic.getPrimaryPath().orElse(""));

        // And adding a primary context URL (topic) it must be returned instead
        setField(topic, "cachedUrls", Set.of(cachedNonPrimaryContextUrl, cachedNonPrimarySubjectUrl, cachedPrimarySubjectUrl, cachedPrimaryContextUrl));
        assertEquals("/topic/primary", topic.getPrimaryPath().orElse(""));

        // Order must not matter
        setField(topic, "cachedUrls", Set.of(cachedNonPrimaryContextUrl, cachedPrimaryContextUrl, cachedPrimarySubjectUrl, cachedNonPrimarySubjectUrl));
        assertEquals("/topic/primary", topic.getPrimaryPath().orElse(""));
    }

    @Test
    public void getAddAndRemoveSubjectTopics() {
        final var topic = spy(this.topic);

        assertEquals(0, topic.getSubjectTopics().size());

        final var subjectTopic1 = mock(SubjectTopic.class);
        final var subjectTopic2 = mock(SubjectTopic.class);
        when(subjectTopic1.isPrimary()).thenReturn(true);
        when(subjectTopic2.isPrimary()).thenReturn(false);

        topic.addSubjectTopic(subjectTopic1);
        verify(subjectTopic1).setTopic(topic);
        when(subjectTopic1.getTopic()).thenReturn(Optional.of(topic));

        assertEquals(1, topic.getSubjectTopics().size());
        assertTrue(topic.getSubjectTopics().contains(subjectTopic1));

        topic.addSubjectTopic(subjectTopic2);
        verify(subjectTopic2).setTopic(topic);
        when(subjectTopic2.getTopic()).thenReturn(Optional.of(topic));

        assertEquals(2, topic.getSubjectTopics().size());
        assertTrue(topic.getSubjectTopics().containsAll(Set.of(subjectTopic1, subjectTopic2)));

        topic.removeSubjectTopic(subjectTopic1);
        assertEquals(1, topic.getSubjectTopics().size());
        assertTrue(topic.getSubjectTopics().contains(subjectTopic2));
        verify(subjectTopic1).setTopic(null);

        topic.removeSubjectTopic(subjectTopic2);
        assertEquals(0, topic.getSubjectTopics().size());
        verify(subjectTopic2).setTopic(null);
    }

    @Test
    public void addGetAndRemoveChildrenTopicSubtopics() {
        assertEquals(0, topic.getChildrenTopicSubtopics().size());

        final var topicSubtopic1 = mock(TopicSubtopic.class);
        final var topicSubtopic2 = mock(TopicSubtopic.class);

        topic.addChildTopicSubtopic(topicSubtopic1);
        verify(topicSubtopic1).setTopic(topic);
        when(topicSubtopic1.getTopic()).thenReturn(Optional.of(topic));
        assertEquals(1, topic.getChildrenTopicSubtopics().size());
        assertTrue(topic.getChildrenTopicSubtopics().contains(topicSubtopic1));

        topic.addChildTopicSubtopic(topicSubtopic2);
        verify(topicSubtopic2).setTopic(topic);
        when(topicSubtopic2.getTopic()).thenReturn(Optional.of(topic));
        assertEquals(2, topic.getChildrenTopicSubtopics().size());
        assertTrue(topic.getChildrenTopicSubtopics().containsAll(Set.of(topicSubtopic1, topicSubtopic2)));

        topic.removeChildTopicSubTopic(topicSubtopic1);
        verify(topicSubtopic1).setTopic(null);
        assertEquals(1, topic.getChildrenTopicSubtopics().size());
        assertTrue(topic.getChildrenTopicSubtopics().contains(topicSubtopic2));

        topic.removeChildTopicSubTopic(topicSubtopic2);
        verify(topicSubtopic2).setTopic(null);
        assertEquals(0, topic.getChildrenTopicSubtopics().size());
    }

    @Test
    public void addGetAndRemoveParentTopicSubtopics() {
        assertEquals(0, topic.getParentTopicSubtopics().size());

        final var topicSubtopic1 = mock(TopicSubtopic.class);
        final var topicSubtopic2 = mock(TopicSubtopic.class);

        topic.addParentTopicSubtopic(topicSubtopic1);
        verify(topicSubtopic1).setSubtopic(topic);
        when(topicSubtopic1.getSubtopic()).thenReturn(Optional.of(topic));
        assertEquals(1, topic.getParentTopicSubtopics().size());
        assertTrue(topic.getParentTopicSubtopics().contains(topicSubtopic1));

        topic.addParentTopicSubtopic(topicSubtopic2);
        verify(topicSubtopic2).setSubtopic(topic);
        when(topicSubtopic2.getSubtopic()).thenReturn(Optional.of(topic));
        assertEquals(2, topic.getParentTopicSubtopics().size());
        assertTrue(topic.getParentTopicSubtopics().containsAll(Set.of(topicSubtopic1, topicSubtopic2)));

        topic.removeParentTopicSubtopic(topicSubtopic1);
        verify(topicSubtopic1).setSubtopic(null);
        assertEquals(1, topic.getParentTopicSubtopics().size());
        assertTrue(topic.getParentTopicSubtopics().contains(topicSubtopic2));

        topic.removeParentTopicSubtopic(topicSubtopic2);
        verify(topicSubtopic2).setSubtopic(null);
        assertEquals(0, topic.getParentTopicSubtopics().size());

    }

    @Test
    public void addGetAndRemoveTopicResources() {
        assertEquals(0, topic.getTopicResources().size());

        final var topicResource1 = mock(TopicResource.class);
        final var topicResource2 = mock(TopicResource.class);

        topic.addTopicResource(topicResource1);
        verify(topicResource1).setTopic(topic);
        when(topicResource1.getTopic()).thenReturn(Optional.of(topic));
        assertEquals(1, topic.getTopicResources().size());
        assertTrue(topic.getTopicResources().contains(topicResource1));

        topic.addTopicResource(topicResource2);
        verify(topicResource2).setTopic(topic);
        when(topicResource2.getTopic()).thenReturn(Optional.of(topic));
        assertEquals(2, topic.getTopicResources().size());
        assertTrue(topic.getTopicResources().containsAll(Set.of(topicResource1, topicResource2)));

        topic.removeTopicResource(topicResource1);
        verify(topicResource1).setTopic(null);
        assertEquals(1, topic.getTopicResources().size());
        assertTrue(topic.getTopicResources().contains(topicResource2));

        topic.removeTopicResource(topicResource2);
        verify(topicResource2).setTopic(null);
        assertEquals(0, topic.getTopicResources().size());
    }

    @Test
    public void addGetAndRemoveTopicResourceTypes() {
        assertEquals(0, topic.getTopicResourceTypes().size());

        final var topicResourceType1 = mock(TopicResourceType.class);
        final var topicResourceType2 = mock(TopicResourceType.class);

        topic.addTopicResourceType(topicResourceType1);
        verify(topicResourceType1).setTopic(topic);
        when(topicResourceType1.getTopic()).thenReturn(Optional.of(topic));
        assertEquals(1, topic.getTopicResourceTypes().size());
        assertTrue(topic.getTopicResourceTypes().contains(topicResourceType1));

        topic.addTopicResourceType(topicResourceType2);
        verify(topicResourceType2).setTopic(topic);
        when(topicResourceType2.getTopic()).thenReturn(Optional.of(topic));
        assertEquals(2, topic.getTopicResourceTypes().size());
        assertTrue(topic.getTopicResourceTypes().containsAll(Set.of(topicResourceType1, topicResourceType2)));

        topic.removeTopicResourceType(topicResourceType1);
        verify(topicResourceType1).setTopic(null);
        assertEquals(1, topic.getTopicResourceTypes().size());
        assertTrue(topic.getTopicResourceTypes().contains(topicResourceType2));

        topic.removeTopicResourceType(topicResourceType2);
        verify(topicResourceType2).setTopic(null);
        assertEquals(0, topic.getTopicResourceTypes().size());
    }

    @Test
    public void addGetAndRemoveSubtopics() {
        final var subtopic1 = mock(Topic.class);
        final var subtopic2 = mock(Topic.class);
        final var subtopic3 = mock(Topic.class);

        final var topicSubtopic1 = new AtomicReference<TopicSubtopic>(null);
        final var topicSubtopic2 = new AtomicReference<TopicSubtopic>(null);
        final var topicSubtopic3 = new AtomicReference<TopicSubtopic>(null);

        doAnswer(invocationOnMock -> {
            topicSubtopic1.set(invocationOnMock.getArgument(0));
            return null;
        }).when(subtopic1).addParentTopicSubtopic(any(TopicSubtopic.class));

        doAnswer(invocationOnMock -> {
            topicSubtopic2.set(invocationOnMock.getArgument(0));
            return null;
        }).when(subtopic2).addParentTopicSubtopic(any(TopicSubtopic.class));

        doAnswer(invocationOnMock -> {
            topicSubtopic3.set(invocationOnMock.getArgument(0));
            return null;
        }).when(subtopic3).addParentTopicSubtopic(any(TopicSubtopic.class));

        topic.addSubtopic(subtopic1, false);
        assertEquals(1, topic.getSubtopics().size());
        assertTrue(topic.getSubtopics().contains(subtopic1));
        assertNotNull(topicSubtopic1.get());
        assertEquals(topic, topicSubtopic1.get().getTopic().orElse(null));
        assertEquals(subtopic1, topicSubtopic1.get().getSubtopic().orElse(null));

        verify(subtopic1).addParentTopicSubtopic(any(TopicSubtopic.class));
        assertEquals(topic, topicSubtopic1.get().getTopic().orElse(null));
        assertEquals(subtopic1, topicSubtopic1.get().getSubtopic().orElse(null));
        verify(subtopic1, never()).setPrimaryParentTopic(any(Topic.class));

        topic.addSubtopic(subtopic2, true);
        assertEquals(2, topic.getSubtopics().size());
        assertTrue(topic.getSubtopics().containsAll(Set.of(subtopic1, subtopic2)));

        verify(subtopic2).addParentTopicSubtopic(any(TopicSubtopic.class));
        assertEquals(topic, topicSubtopic2.get().getTopic().orElse(null));
        assertEquals(subtopic2, topicSubtopic2.get().getSubtopic().orElse(null));
        verify(subtopic2, times(1)).setPrimaryParentTopic(topic);

        // Default is primary subtopic
        topic.addSubtopic(subtopic3);
        assertEquals(3, topic.getSubtopics().size());
        assertTrue(topic.getSubtopics().containsAll(Set.of(subtopic1, subtopic2, subtopic3)));

        verify(subtopic3).addParentTopicSubtopic(any(TopicSubtopic.class));
        verify(subtopic3, times(1)).setPrimaryParentTopic(topic);

        topicSubtopic3.get().setPrimary(true);

        // Trying to add again throws exception
        try {
            topic.addSubtopic(subtopic2);
            fail("Expected DuplicateIdException");
        } catch (DuplicateIdException ignored) {
        }

        verify(subtopic3, never()).setRandomPrimaryTopic();

        topic.removeSubtopic(subtopic3);

        verify(subtopic3, atLeastOnce()).setRandomPrimaryTopic();
        assertEquals(2, topic.getSubtopics().size());
        assertTrue(topic.getSubtopics().containsAll(Set.of(subtopic1, subtopic2)));

        topic.removeSubtopic(subtopic1);
        verify(subtopic1, never()).setRandomPrimaryTopic();
        assertEquals(1, topic.getSubtopics().size());
        assertTrue(topic.getSubtopics().contains(subtopic2));

        topic.removeSubtopic(subtopic2);
        verify(subtopic2, never()).setRandomPrimaryTopic();
        assertEquals(0, topic.getSubtopics().size());
    }

    @Test
    public void hasSingleSubject() {
        assertFalse(topic.hasSingleSubject());
        topic.addSubjectTopic(mock(SubjectTopic.class));
        assertTrue(topic.hasSingleSubject());
        topic.addSubjectTopic(mock(SubjectTopic.class));
        assertFalse(topic.hasSingleSubject());
    }

    @Test
    public void getPrimaryParentTopic() {
        final var parentTopic1 = mock(Topic.class);
        final var parentTopic2 = mock(Topic.class);
        final var parentTopic3 = mock(Topic.class);

        final var parentTopicSubtopic1 = mock(TopicSubtopic.class);
        final var parentTopicSubtopic2 = mock(TopicSubtopic.class);
        final var parentTopicSubtopic3 = mock(TopicSubtopic.class);

        Set.of(parentTopicSubtopic1, parentTopicSubtopic2, parentTopicSubtopic3)
                .forEach(parentTopicSubtopic -> when(parentTopicSubtopic.getSubtopic()).thenReturn(Optional.of(topic)));

        when(parentTopicSubtopic1.getTopic()).thenReturn(Optional.of(parentTopic1));
        when(parentTopicSubtopic2.getTopic()).thenReturn(Optional.of(parentTopic2));
        when(parentTopicSubtopic3.getTopic()).thenReturn(Optional.of(parentTopic3));

        when(parentTopicSubtopic2.isPrimary()).thenReturn(true);

        assertFalse(topic.getPrimaryParentTopic().isPresent());
        topic.addParentTopicSubtopic(parentTopicSubtopic1);
        assertFalse(topic.getPrimaryParentTopic().isPresent());

        topic.addParentTopicSubtopic(parentTopicSubtopic2);
        assertTrue(topic.getPrimaryParentTopic().isPresent());
        assertEquals(parentTopic2, topic.getPrimaryParentTopic().orElse(null));

        topic.addParentTopicSubtopic(parentTopicSubtopic3);
        assertTrue(topic.getPrimaryParentTopic().isPresent());
        assertEquals(parentTopic2, topic.getPrimaryParentTopic().orElse(null));
    }

    @Test
    public void setPrimaryParentTopic() {
        final var parentTopic1 = mock(Topic.class);
        final var parentTopic2 = mock(Topic.class);
        final var parentTopic3 = mock(Topic.class);

        final var parentTopicSubtopic1 = mock(TopicSubtopic.class);
        final var parentTopicSubtopic2 = mock(TopicSubtopic.class);
        final var parentTopicSubtopic3 = mock(TopicSubtopic.class);

        Set.of(parentTopicSubtopic1, parentTopicSubtopic2, parentTopicSubtopic3)
                .forEach(parentTopicSubtopic -> when(parentTopicSubtopic.getSubtopic()).thenReturn(Optional.of(topic)));

        when(parentTopicSubtopic1.getTopic()).thenReturn(Optional.of(parentTopic1));
        when(parentTopicSubtopic2.getTopic()).thenReturn(Optional.of(parentTopic2));
        when(parentTopicSubtopic3.getTopic()).thenReturn(Optional.of(parentTopic3));

        topic.addParentTopicSubtopic(parentTopicSubtopic1);
        topic.addParentTopicSubtopic(parentTopicSubtopic2);
        topic.addParentTopicSubtopic(parentTopicSubtopic3);

        topic.setPrimaryParentTopic(parentTopic1);
        verify(parentTopicSubtopic1).setPrimary(true);
        verify(parentTopicSubtopic2).setPrimary(false);
        verify(parentTopicSubtopic3).setPrimary(false);

        reset(parentTopicSubtopic1, parentTopicSubtopic2, parentTopicSubtopic3);

        Set.of(parentTopicSubtopic1, parentTopicSubtopic2, parentTopicSubtopic3)
                .forEach(parentTopicSubtopic -> when(parentTopicSubtopic.getSubtopic()).thenReturn(Optional.of(topic)));

        when(parentTopicSubtopic1.getTopic()).thenReturn(Optional.of(parentTopic1));
        when(parentTopicSubtopic2.getTopic()).thenReturn(Optional.of(parentTopic2));
        when(parentTopicSubtopic3.getTopic()).thenReturn(Optional.of(parentTopic3));

        when(parentTopicSubtopic1.isPrimary()).thenReturn(true);

        topic.setPrimaryParentTopic(parentTopic3);
        verify(parentTopicSubtopic1).setPrimary(false);
        verify(parentTopicSubtopic2).setPrimary(false);
        verify(parentTopicSubtopic3).setPrimary(true);
    }

    @Test
    public void getAndAddResource() {
        final var resource1 = mock(Resource.class);
        final var resource2 = mock(Resource.class);
        final var resource3 = mock(Resource.class);

        final var topicResource1 = new AtomicReference<TopicResource>(null);
        final var topicResource2 = new AtomicReference<TopicResource>(null);
        final var topicResource3 = new AtomicReference<TopicResource>(null);

        doAnswer(invocationOnMock -> {
            topicResource1.set(invocationOnMock.getArgument(0));
            when(resource1.getTopicResources()).thenReturn(new HashSet<>(Set.of(topicResource1.get())));
            return null;
        }).when(resource1).addTopicResource(any(TopicResource.class));
        doAnswer(invocationOnMock -> {
            topicResource2.set(invocationOnMock.getArgument(0));
            when(resource2.getTopicResources()).thenReturn(new HashSet<>(Set.of(topicResource2.get())));
            return null;
        }).when(resource2).addTopicResource(any(TopicResource.class));
        doAnswer(invocationOnMock -> {
            topicResource3.set(invocationOnMock.getArgument(0));
            when(resource3.getTopicResources()).thenReturn(new HashSet<>(Set.of(topicResource3.get())));
            return null;
        }).when(resource3).addTopicResource(any(TopicResource.class));

        doAnswer(invocationOnMock -> {
            topicResource1.get().setPrimary(true);
            return null;
        }).when(resource1).setPrimaryTopic(topic);

        doAnswer(invocationOnMock -> {
            topicResource3.get().setPrimary(true);
            return null;
        }).when(resource3).setPrimaryTopic(topic);

        assertEquals(0, topic.getResources().size());

        topic.addResource(resource1);
        verify(resource1).addTopicResource(any(TopicResource.class));
        assertEquals(1, topic.getResources().size());
        assertTrue(topic.getResources().contains(resource1));
        assertEquals(topic, topicResource1.get().getTopic().orElse(null));
        assertEquals(resource1, topicResource1.get().getResource().orElse(null));
        verify(resource1).setPrimaryTopic(topic);

        topic.addResource(resource2, false);
        verify(resource2).addTopicResource(any(TopicResource.class));
        assertEquals(2, topic.getResources().size());
        assertTrue(topic.getResources().containsAll(Set.of(resource1, resource2)));
        assertEquals(topic, topicResource2.get().getTopic().orElse(null));
        assertEquals(resource2, topicResource2.get().getResource().orElse(null));
        assertFalse(topicResource2.get().isPrimary());
        verify(resource2, never()).setPrimaryTopic(topic);

        topic.addResource(resource3, true);
        verify(resource3).addTopicResource(any(TopicResource.class));
        assertEquals(3, topic.getResources().size());
        assertTrue(topic.getResources().containsAll(Set.of(resource1, resource2, resource3)));
        assertEquals(topic, topicResource3.get().getTopic().orElse(null));
        assertEquals(resource3, topicResource3.get().getResource().orElse(null));
        verify(resource3).setPrimaryTopic(topic);

        topic.removeResource(resource3);
        verify(resource3).setRandomPrimaryTopic();
        assertEquals(2, topic.getResources().size());
        assertTrue(topic.getResources().containsAll(Set.of(resource1, resource2)));

        topic.removeResource(resource1);
        assertEquals(1, topic.getResources().size());
        assertTrue(topic.getResources().contains(resource2));

        topic.removeResource(resource2);
        assertEquals(0, topic.getResources().size());
    }

    @Test
    public void getParentTopics() {
        final var parentTopic1 = mock(Topic.class);
        final var parentTopic2 = mock(Topic.class);

        final var topicSubtopic1 = mock(TopicSubtopic.class);
        final var topicSubtopic2 = mock(TopicSubtopic.class);
        final var topicSubtopic3 = mock(TopicSubtopic.class); // Returns no topic, must not trigger error

        when(topicSubtopic1.getSubtopic()).thenReturn(Optional.of(topic));
        when(topicSubtopic2.getSubtopic()).thenReturn(Optional.of(topic));
        when(topicSubtopic3.getSubtopic()).thenReturn(Optional.of(topic));
        when(topicSubtopic1.getTopic()).thenReturn(Optional.of(parentTopic1));
        when(topicSubtopic2.getTopic()).thenReturn(Optional.of(parentTopic2));
        when(topicSubtopic3.getTopic()).thenReturn(Optional.empty());

        topic.addParentTopicSubtopic(topicSubtopic1);
        topic.addParentTopicSubtopic(topicSubtopic2);
        topic.addParentTopicSubtopic(topicSubtopic3);

        assertEquals(2, topic.getParentTopics().size());
        assertTrue(topic.getParentTopics().containsAll(Set.of(parentTopic1, parentTopic2)));
    }

    @Test
    public void getResources() {
        final var resource1 = mock(Resource.class);
        final var resource2 = mock(Resource.class);

        final var topicResource1 = mock(TopicResource.class);
        final var topicResource2 = mock(TopicResource.class);
        final var topicResource3 = mock(TopicResource.class);

        Set.of(topicResource1, topicResource2, topicResource3).forEach(topicResource -> when(topicResource.getTopic()).thenReturn(Optional.of(topic)));
        when(topicResource1.getResource()).thenReturn(Optional.of(resource1));
        when(topicResource2.getResource()).thenReturn(Optional.of(resource2));

        topic.addTopicResource(topicResource1);
        topic.addTopicResource(topicResource2);
        topic.addTopicResource(topicResource3);

        assertEquals(3, topic.getTopicResources().size());
        assertEquals(2, topic.getResources().size());
        assertTrue(topic.getResources().containsAll(Set.of(resource1, resource2)));
    }

    @Test
    public void setAndGetContentUri() throws URISyntaxException {
        assertNull(topic.getContentUri());
        topic.setContentUri(new URI("urn:test1"));
        assertEquals("urn:test1", topic.getContentUri().toString());
    }

    @Test
    public void setPrimarySubject() {
        final var subject1 = mock(Subject.class);
        final var subject2 = mock(Subject.class);

        final var subjectTopic1 = mock(SubjectTopic.class);
        final var subjectTopic2 = mock(SubjectTopic.class);

        when(subjectTopic1.getTopic()).thenReturn(Optional.of(topic));
        when(subjectTopic2.getTopic()).thenReturn(Optional.of(topic));

        when(subjectTopic1.getSubject()).thenReturn(Optional.of(subject1));
        when(subjectTopic2.getSubject()).thenReturn(Optional.of(subject2));

        topic.addSubjectTopic(subjectTopic1);
        topic.addSubjectTopic(subjectTopic2);

        clearInvocations(subjectTopic1, subjectTopic2);

        topic.setPrimarySubject(subject1);
        verify(subjectTopic1, atLeastOnce()).setPrimary(true);
        verify(subjectTopic1, never()).setPrimary(false);
        verify(subjectTopic2, atLeastOnce()).setPrimary(false);
        verify(subjectTopic2, never()).setPrimary(true);

        clearInvocations(subjectTopic1, subjectTopic2);

        topic.setPrimarySubject(subject2);
        verify(subjectTopic2, atLeastOnce()).setPrimary(true);
        verify(subjectTopic2, never()).setPrimary(false);
        verify(subjectTopic1, atLeastOnce()).setPrimary(false);
        verify(subjectTopic1, never()).setPrimary(true);
    }

    @Test
    public void setRandomPrimarySubject() {
        final var subject1 = mock(Subject.class);
        final var subject2 = mock(Subject.class);

        final var subjectTopic1 = mock(SubjectTopic.class);
        final var subjectTopic2 = mock(SubjectTopic.class);

        when(subjectTopic1.getTopic()).thenReturn(Optional.of(topic));
        when(subjectTopic2.getTopic()).thenReturn(Optional.of(topic));

        when(subjectTopic1.getSubject()).thenReturn(Optional.of(subject1));
        when(subjectTopic2.getSubject()).thenReturn(Optional.of(subject2));

        topic.addSubjectTopic(subjectTopic1);
        topic.addSubjectTopic(subjectTopic2);

        clearInvocations(subjectTopic1, subjectTopic2);

        final var setPrimaryInvoked = new AtomicBoolean(false);

        doAnswer(invocationOnMock -> {
            setPrimaryInvoked.set(true);
            return null;
        }).when(subjectTopic1).setPrimary(true);

        doAnswer(invocationOnMock -> {
            setPrimaryInvoked.set(true);
            return null;
        }).when(subjectTopic2).setPrimary(true);

        topic.setRandomPrimarySubject();

        assertTrue(setPrimaryInvoked.get());
    }

    @Test
    public void setRandomPrimaryTopic() {
        final var topic1 = mock(Topic.class);
        final var topic2 = mock(Topic.class);

        final var topicSubtopic1 = mock(TopicSubtopic.class);
        final var topicSubtopic2 = mock(TopicSubtopic.class);

        when(topicSubtopic1.getSubtopic()).thenReturn(Optional.of(topic));
        when(topicSubtopic2.getSubtopic()).thenReturn(Optional.of(topic));

        when(topicSubtopic1.getTopic()).thenReturn(Optional.of(topic1));
        when(topicSubtopic2.getTopic()).thenReturn(Optional.of(topic2));

        topic.addParentTopicSubtopic(topicSubtopic1);
        topic.addParentTopicSubtopic(topicSubtopic2);

        clearInvocations(topicSubtopic1, topicSubtopic2);

        final var setPrimaryInvoked = new AtomicBoolean(false);

        doAnswer(invocationOnMock -> {
            setPrimaryInvoked.set(true);
            return null;
        }).when(topicSubtopic1).setPrimary(true);

        doAnswer(invocationOnMock -> {
            setPrimaryInvoked.set(true);
            return null;
        }).when(topicSubtopic2).setPrimary(true);

        topic.setRandomPrimaryTopic();

        assertTrue(setPrimaryInvoked.get());
    }

    @Test
    public void addFilter() {
        final var filter1 = mock(Filter.class);
        final var filter2 = mock(Filter.class);
        final var filter3 = mock(Filter.class);

        final var relevance1 = mock(Relevance.class);
        final var relevance2 = mock(Relevance.class);
        final var relevance3 = mock(Relevance.class);

        assertEquals(0, topic.getTopicFilters().size());

        final var topicFilter1 = topic.addFilter(filter1, relevance1);
        assertNotNull(topicFilter1);

        assertEquals(1, topic.getTopicFilters().size());
        assertTrue(topic.getTopicFilters().contains(topicFilter1));

        assertSame(topic, topicFilter1.getTopic().orElse(null));
        assertSame(filter1, topicFilter1.getFilter().orElse(null));
        assertSame(relevance1, topicFilter1.getRelevance().orElse(null));

        final var topicFilter2 = topic.addFilter(filter2, relevance2);
        assertNotNull(topicFilter2);

        assertEquals(2, topic.getTopicFilters().size());
        assertTrue(topic.getTopicFilters().containsAll(Set.of(topicFilter1, topicFilter2)));

        assertSame(topic, topicFilter2.getTopic().orElse(null));
        assertSame(filter2, topicFilter2.getFilter().orElse(null));
        assertSame(relevance2, topicFilter2.getRelevance().orElse(null));

        final var topicFilter3 = topic.addFilter(filter3, relevance2);
        assertNotNull(topicFilter3);

        assertEquals(3, topic.getTopicFilters().size());
        assertTrue(topic.getTopicFilters().containsAll(Set.of(topicFilter1, topicFilter2, topicFilter3)));

        assertSame(topic, topicFilter3.getTopic().orElse(null));
        assertSame(filter3, topicFilter3.getFilter().orElse(null));
        assertSame(relevance2, topicFilter3.getRelevance().orElse(null));

        final var topicFilter4 = topic.addFilter(filter2, relevance3);
        assertNotNull(topicFilter4);

        assertEquals(4, topic.getTopicFilters().size());
        assertTrue(topic.getTopicFilters().containsAll(Set.of(topicFilter1, topicFilter2, topicFilter3, topicFilter4)));

        assertSame(topic, topicFilter4.getTopic().orElse(null));
        assertSame(filter2, topicFilter4.getFilter().orElse(null));
        assertSame(relevance3, topicFilter4.getRelevance().orElse(null));
    }

    @Test
    public void addAndRemoveResourceType() {
        final var resourceType1 = mock(ResourceType.class);
        final var resourceType2 = mock(ResourceType.class);

        assertEquals(0, topic.getTopicResourceTypes().size());

        final var topicResourceType1 = topic.addResourceType(resourceType1);

        assertEquals(1, topic.getTopicResourceTypes().size());
        assertTrue(topic.getTopicResourceTypes().contains(topicResourceType1));

        assertSame(topic, topicResourceType1.getTopic().orElse(null));
        assertSame(resourceType1, topicResourceType1.getResourceType().orElse(null));

        final var topicResourceType2 = topic.addResourceType(resourceType2);

        assertEquals(2, topic.getTopicResourceTypes().size());
        assertTrue(topic.getTopicResourceTypes().containsAll(Set.of(topicResourceType1, topicResourceType2)));

        assertSame(topic, topicResourceType2.getTopic().orElse(null));
        assertSame(resourceType2, topicResourceType2.getResourceType().orElse(null));

        topic.removeResourceType(resourceType1);
        assertEquals(1, topic.getTopicResourceTypes().size());
        assertTrue(topic.getTopicResourceTypes().contains(topicResourceType2));

        assertFalse(topicResourceType1.getTopic().isPresent());

        // Trying to remove again triggers exception
        try {
            topic.removeResourceType(resourceType1);
            fail("Expected ChildNotFoundException");
        } catch (ChildNotFoundException ignored) {

        }

        topic.removeResourceType(resourceType2);
        assertEquals(0, topic.getTopicResourceTypes().size());
        assertFalse(topicResourceType2.getTopic().isPresent());
    }

    @Test
    public void getAddAndRemoveTopicFilter() {
        final var topicFilter1 = mock(TopicFilter.class);
        final var topicFilter2 = mock(TopicFilter.class);
        final var topicFilter3 = mock(TopicFilter.class);

        assertEquals(0, topic.getTopicFilters().size());

        topic.addTopicFilter(topicFilter1);
        assertEquals(1, topic.getTopicFilters().size());
        assertTrue(topic.getTopicFilters().contains(topicFilter1));

        verify(topicFilter1).setTopic(topic);

        topic.addTopicFilter(topicFilter2);
        assertEquals(2, topic.getTopicFilters().size());
        assertTrue(topic.getTopicFilters().containsAll(Set.of(topicFilter1, topicFilter2)));

        verify(topicFilter2).setTopic(topic);

        topic.addTopicFilter(topicFilter3);
        assertEquals(3, topic.getTopicFilters().size());
        assertTrue(topic.getTopicFilters().containsAll(Set.of(topicFilter1, topicFilter2, topicFilter3)));

        verify(topicFilter3).setTopic(topic);

        final var mockTopic = mock(Topic.class);

        when(topicFilter1.getTopic()).thenReturn(Optional.of(topic));
        when(topicFilter2.getTopic()).thenReturn(Optional.of(topic));
        when(topicFilter3.getTopic()).thenReturn(Optional.of(mockTopic));

        clearInvocations(topicFilter1, topicFilter2, topicFilter3);

        topic.removeTopicFilter(topicFilter1);
        assertEquals(2, topic.getTopicFilters().size());
        assertTrue(topic.getTopicFilters().containsAll(Set.of(topicFilter2, topicFilter3)));

        verify(topicFilter1).setTopic(null);

        topic.removeTopicFilter(topicFilter2);
        assertEquals(1, topic.getTopicFilters().size());
        assertTrue(topic.getTopicFilters().contains(topicFilter3));

        verify(topicFilter2).setTopic(null);

        topic.removeTopicFilter(topicFilter3);
        assertEquals(0, topic.getTopicFilters().size());
        verify(topicFilter3, never()).setTopic(null);
    }

    @Test
    public void setAndIsContext() {
        assertFalse(topic.isContext());
        topic.setContext(true);
        assertTrue(topic.isContext());
        topic.setContext(false);
        assertFalse(topic.isContext());
    }

    @Test
    public void addAndGetAndRemoveTranslation() {
        assertEquals(0, topic.getTranslations().size());

        var returnedTranslation = topic.addTranslation("nb");
        assertEquals(1, topic.getTranslations().size());
        assertEquals("nb", returnedTranslation.getLanguageCode());
        assertTrue(topic.getTranslations().contains(returnedTranslation));
        assertEquals(topic, returnedTranslation.getTopic());

        var returnedTranslation2 = topic.addTranslation("en");
        assertEquals(2, topic.getTranslations().size());
        assertEquals("en", returnedTranslation2.getLanguageCode());
        assertTrue(topic.getTranslations().containsAll(Set.of(returnedTranslation, returnedTranslation2)));
        assertEquals(topic, returnedTranslation2.getTopic());

        topic.removeTranslation("nb");

        assertNull(returnedTranslation.getTopic());
        assertFalse(topic.getTranslations().contains(returnedTranslation));

        assertFalse(topic.getTranslation("nb").isPresent());

        topic.addTranslation(returnedTranslation);
        assertEquals(topic, returnedTranslation.getTopic());
        assertTrue(topic.getTranslations().contains(returnedTranslation));

        assertEquals(returnedTranslation, topic.getTranslation("nb").get());
        assertEquals(returnedTranslation2, topic.getTranslation("en").orElse(null));
    }

    @Test
    public void preRemove() {
        final var parentTopicSubtopics = Set.of(mock(TopicSubtopic.class), mock(TopicSubtopic.class));
        final var childTopicSubtopics = Set.of(mock(TopicSubtopic.class), mock(TopicSubtopic.class));
        final var subjectTopics = Set.of(mock(SubjectTopic.class), mock(SubjectTopic.class));
        final var topicResources = Set.of(mock(TopicResource.class), mock(TopicResource.class));
        final var topicFilters = Set.of(mock(TopicFilter.class), mock(TopicFilter.class));

        final var allMocks = Set.of(parentTopicSubtopics, childTopicSubtopics, subjectTopics, topicResources, topicFilters)
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        parentTopicSubtopics.forEach(topicSubtopic -> {
            topic.addParentTopicSubtopic(topicSubtopic);
            when(topicSubtopic.getSubtopic()).thenReturn(Optional.of(topic));
        });

        childTopicSubtopics.forEach(topicSubtopic -> {
            topic.addChildTopicSubtopic(topicSubtopic);
            when(topicSubtopic.getTopic()).thenReturn(Optional.of(topic));
        });

        subjectTopics.forEach(subjectTopic -> {
            topic.addSubjectTopic(subjectTopic);
            when(subjectTopic.getTopic()).thenReturn(Optional.of(topic));
        });

        topicResources.forEach(topicResource -> {
            topic.addTopicResource(topicResource);
            when(topicResource.getTopic()).thenReturn(Optional.of(topic));
        });

        topicFilters.forEach(topicFilter -> {
            topic.addTopicFilter(topicFilter);
            when(topicFilter.getTopic()).thenReturn(Optional.of(topic));
        });

        allMocks.forEach(Mockito::clearInvocations);

        topic.preRemove();

        parentTopicSubtopics.forEach(topicSubtopic -> verify(topicSubtopic).setSubtopic(null));
        childTopicSubtopics.forEach(topicSubtopic -> verify(topicSubtopic).setTopic(null));
        subjectTopics.forEach(subjectTopic -> verify(subjectTopic).setTopic(null));
        topicResources.forEach(topicResource -> verify(topicResource).setTopic(null));
        topicFilters.forEach(topicFilter -> verify(topicFilter).setTopic(null));
    }

}