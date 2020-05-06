package no.ndla.taxonomy.domain;

import no.ndla.taxonomy.domain.exceptions.ChildNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

public class TopicTest {
    private Topic topic;

    @BeforeEach
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

        final var cachedPrimaryContextUrl = mock(CachedPath.class);
        final var cachedPrimarySubjectUrl = mock(CachedPath.class);

        when(cachedPrimaryContextUrl.getPath()).thenReturn("/topic/primary");
        when(cachedPrimarySubjectUrl.getPath()).thenReturn("/subject/primary");

        // If returning a primary subject path along it must return it
        setField(topic, "cachedPaths", Set.of(cachedPrimarySubjectUrl));
        assertEquals("/subject/primary", topic.getPrimaryPath().orElse(""));

        // And adding a primary context URL (topic) it must be returned instead
        setField(topic, "cachedPaths", Set.of(cachedPrimarySubjectUrl, cachedPrimaryContextUrl));
        assertEquals("/topic/primary", topic.getPrimaryPath().orElse(""));

        // Order must not matter
        setField(topic, "cachedPaths", Set.of(cachedPrimaryContextUrl, cachedPrimarySubjectUrl));
        assertEquals("/topic/primary", topic.getPrimaryPath().orElse(""));
    }

    @Test
    public void getAddAndRemoveSubjectTopics() {
        final var topic = spy(this.topic);

        assertEquals(0, topic.getSubjectTopics().size());

        final var subjectTopic1 = mock(SubjectTopic.class);
        final var subjectTopic2 = mock(SubjectTopic.class);

        try {
            topic.addSubjectTopic(subjectTopic1);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {

        }

        when(subjectTopic1.getTopic()).thenReturn(Optional.of(topic));
        topic.addSubjectTopic(subjectTopic1);

        when(subjectTopic1.getTopic()).thenReturn(Optional.of(topic));

        assertEquals(1, topic.getSubjectTopics().size());
        assertTrue(topic.getSubjectTopics().contains(subjectTopic1));

        try {
            topic.addSubjectTopic(subjectTopic2);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }

        when(subjectTopic2.getTopic()).thenReturn(Optional.of(topic));
        topic.addSubjectTopic(subjectTopic2);

        when(subjectTopic2.getTopic()).thenReturn(Optional.of(topic));

        assertEquals(2, topic.getSubjectTopics().size());
        assertTrue(topic.getSubjectTopics().containsAll(Set.of(subjectTopic1, subjectTopic2)));

        topic.removeSubjectTopic(subjectTopic1);
        assertEquals(1, topic.getSubjectTopics().size());
        assertTrue(topic.getSubjectTopics().contains(subjectTopic2));
        verify(subjectTopic1).disassociate();

        topic.removeSubjectTopic(subjectTopic2);
        assertEquals(0, topic.getSubjectTopics().size());
        verify(subjectTopic2).disassociate();
    }

    @Test
    public void addGetAndRemoveChildrenTopicSubtopics() {
        assertEquals(0, topic.getChildrenTopicSubtopics().size());

        final var topicSubtopic1 = mock(TopicSubtopic.class);
        final var topicSubtopic2 = mock(TopicSubtopic.class);

        try {
            topic.addChildTopicSubtopic(topicSubtopic1);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {

        }
        when(topicSubtopic1.getTopic()).thenReturn(Optional.of(topic));
        topic.addChildTopicSubtopic(topicSubtopic1);

        assertEquals(1, topic.getChildrenTopicSubtopics().size());
        assertTrue(topic.getChildrenTopicSubtopics().contains(topicSubtopic1));

        try {
            topic.addChildTopicSubtopic(topicSubtopic2);
        } catch (IllegalArgumentException ignored) {
        }
        when(topicSubtopic2.getTopic()).thenReturn(Optional.of(topic));
        topic.addChildTopicSubtopic(topicSubtopic2);

        assertEquals(2, topic.getChildrenTopicSubtopics().size());
        assertTrue(topic.getChildrenTopicSubtopics().containsAll(Set.of(topicSubtopic1, topicSubtopic2)));

        topic.removeChildTopicSubTopic(topicSubtopic1);
        verify(topicSubtopic1).disassociate();
        assertEquals(1, topic.getChildrenTopicSubtopics().size());
        assertTrue(topic.getChildrenTopicSubtopics().contains(topicSubtopic2));

        topic.removeChildTopicSubTopic(topicSubtopic2);
        verify(topicSubtopic2).disassociate();
        assertEquals(0, topic.getChildrenTopicSubtopics().size());
    }

    @Test
    public void addGetAndRemoveParentTopicSubtopics() {
        assertFalse(topic.getParentTopicSubtopic().isPresent());

        final var topicSubtopic1 = mock(TopicSubtopic.class);
        final var topicSubtopic2 = mock(TopicSubtopic.class);

        try {
            topic.addParentTopicSubtopic(topicSubtopic1);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }
        when(topicSubtopic1.getSubtopic()).thenReturn(Optional.of(topic));
        topic.addParentTopicSubtopic(topicSubtopic1);


        assertTrue(topic.getParentTopicSubtopic().isPresent());
        assertSame(topicSubtopic1, topic.getParentTopicSubtopic().orElseThrow());

        try {
            topic.addParentTopicSubtopic(topicSubtopic2);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }

        topic.removeParentTopicSubtopic(topicSubtopic1);
        verify(topicSubtopic1).disassociate();
        assertFalse(topic.getParentTopicSubtopic().isPresent());
    }

    @Test
    public void addGetAndRemoveTopicResources() {
        assertEquals(0, topic.getTopicResources().size());

        final var topicResource1 = mock(TopicResource.class);
        final var topicResource2 = mock(TopicResource.class);

        try {
            topic.addTopicResource(topicResource1);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }

        when(topicResource1.getTopic()).thenReturn(Optional.of(topic));
        topic.addTopicResource(topicResource1);

        assertEquals(1, topic.getTopicResources().size());
        assertTrue(topic.getTopicResources().contains(topicResource1));

        try {
            topic.addTopicResource(topicResource2);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }
        when(topicResource2.getTopic()).thenReturn(Optional.of(topic));
        topic.addTopicResource(topicResource2);

        assertEquals(2, topic.getTopicResources().size());
        assertTrue(topic.getTopicResources().containsAll(Set.of(topicResource1, topicResource2)));

        topic.removeTopicResource(topicResource1);
        verify(topicResource1).disassociate();
        assertEquals(1, topic.getTopicResources().size());
        assertTrue(topic.getTopicResources().contains(topicResource2));

        topic.removeTopicResource(topicResource2);
        verify(topicResource2).disassociate();
        assertEquals(0, topic.getTopicResources().size());
    }

    @Test
    public void addGetAndRemoveTopicResourceTypes() {
        assertEquals(0, topic.getTopicResourceTypes().size());

        final var topicResourceType1 = mock(TopicResourceType.class);
        final var topicResourceType2 = mock(TopicResourceType.class);

        try {
            topic.addTopicResourceType(topicResourceType1);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }
        when(topicResourceType1.getTopic()).thenReturn(Optional.of(topic));
        topic.addTopicResourceType(topicResourceType1);

        assertEquals(1, topic.getTopicResourceTypes().size());
        assertTrue(topic.getTopicResourceTypes().contains(topicResourceType1));

        try {
            topic.addTopicResourceType(topicResourceType2);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }
        when(topicResourceType2.getTopic()).thenReturn(Optional.of(topic));
        topic.addTopicResourceType(topicResourceType2);

        assertEquals(2, topic.getTopicResourceTypes().size());
        assertTrue(topic.getTopicResourceTypes().containsAll(Set.of(topicResourceType1, topicResourceType2)));

        topic.removeTopicResourceType(topicResourceType1);
        verify(topicResourceType1).disassociate();
        assertEquals(1, topic.getTopicResourceTypes().size());
        assertTrue(topic.getTopicResourceTypes().contains(topicResourceType2));

        topic.removeTopicResourceType(topicResourceType2);
        verify(topicResourceType2).disassociate();
        assertEquals(0, topic.getTopicResourceTypes().size());
    }

    @Test
    public void getParentTopic() {
        final var parentTopic1 = mock(Topic.class);

        final var topicSubtopic1 = mock(TopicSubtopic.class);

        when(topicSubtopic1.getSubtopic()).thenReturn(Optional.of(topic));
        when(topicSubtopic1.getTopic()).thenReturn(Optional.of(parentTopic1));

        assertFalse(topic.getParentTopic().isPresent());

        topic.addParentTopicSubtopic(topicSubtopic1);

        assertTrue(topic.getParentTopic().isPresent());
        assertSame(parentTopic1, topic.getParentTopic().orElseThrow());
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

        try {
            topic.addTopicFilter(topicFilter1);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }
        when(topicFilter1.getTopic()).thenReturn(Optional.of(topic));
        topic.addTopicFilter(topicFilter1);

        assertEquals(1, topic.getTopicFilters().size());
        assertTrue(topic.getTopicFilters().contains(topicFilter1));

        try {
            topic.addTopicFilter(topicFilter2);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }
        when(topicFilter2.getTopic()).thenReturn(Optional.of(topic));
        topic.addTopicFilter(topicFilter2);

        assertEquals(2, topic.getTopicFilters().size());
        assertTrue(topic.getTopicFilters().containsAll(Set.of(topicFilter1, topicFilter2)));

        try {
            topic.addTopicFilter(topicFilter3);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }
        when(topicFilter3.getTopic()).thenReturn(Optional.of(topic));
        topic.addTopicFilter(topicFilter3);

        assertEquals(3, topic.getTopicFilters().size());
        assertTrue(topic.getTopicFilters().containsAll(Set.of(topicFilter1, topicFilter2, topicFilter3)));

        final var mockTopic = mock(Topic.class);

        when(topicFilter1.getTopic()).thenReturn(Optional.of(topic));
        when(topicFilter2.getTopic()).thenReturn(Optional.of(topic));
        when(topicFilter3.getTopic()).thenReturn(Optional.of(mockTopic));

        clearInvocations(topicFilter1, topicFilter2, topicFilter3);

        topic.removeTopicFilter(topicFilter1);
        assertEquals(2, topic.getTopicFilters().size());
        assertTrue(topic.getTopicFilters().containsAll(Set.of(topicFilter2, topicFilter3)));

        verify(topicFilter1).disassociate();

        topic.removeTopicFilter(topicFilter2);
        assertEquals(1, topic.getTopicFilters().size());
        assertTrue(topic.getTopicFilters().contains(topicFilter3));

        verify(topicFilter2).disassociate();

        topic.removeTopicFilter(topicFilter3);
        assertEquals(0, topic.getTopicFilters().size());
        verify(topicFilter3, never()).disassociate();
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

        parentTopicSubtopics.forEach(topicSubtopic -> {
            when(topicSubtopic.getSubtopic()).thenReturn(Optional.of(topic));
            topic.addParentTopicSubtopic(topicSubtopic);
        });

        childTopicSubtopics.forEach(topicSubtopic -> {
            when(topicSubtopic.getTopic()).thenReturn(Optional.of(topic));
            topic.addChildTopicSubtopic(topicSubtopic);
        });

        subjectTopics.forEach(subjectTopic -> {
            when(subjectTopic.getTopic()).thenReturn(Optional.of(topic));
            topic.addSubjectTopic(subjectTopic);
        });

        topicResources.forEach(topicResource -> {
            when(topicResource.getTopic()).thenReturn(Optional.of(topic));
            topic.addTopicResource(topicResource);
        });

        topicFilters.forEach(topicFilter -> {
            when(topicFilter.getTopic()).thenReturn(Optional.of(topic));
            topic.addTopicFilter(topicFilter);
        });

        topic.preRemove();

        parentTopicSubtopics.forEach(topicSubtopic -> verify(topicSubtopic).disassociate());
        childTopicSubtopics.forEach(topicSubtopic -> verify(topicSubtopic).disassociate());
        subjectTopics.forEach(subjectTopic -> verify(subjectTopic).disassociate());
        topicResources.forEach(topicResource -> verify(topicResource).disassociate());
        topicFilters.forEach(topicFilter -> verify(topicFilter).disassociate());
    }

    @Test
    public void getCachedPaths() {
        final var cachedPaths = Set.of();

        setField(topic, "cachedPaths", cachedPaths);
        assertSame(cachedPaths, topic.getCachedPaths());
    }
}