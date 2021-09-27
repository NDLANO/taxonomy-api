package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Resource;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.domain.TopicResource;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResourceTreeSortableTest {
    @Test
    public void getSortableRank() {

        // TopicResource
        var sortable = new ResourceTreeSortable<Topic>("resource", "topic", 100, 10, 1000);
        assertEquals(1000 - 10000, sortable.getSortableRank());

        // TopicSubtopic
        sortable = new ResourceTreeSortable<Topic>("topic", "topic", 100, 10, 800);
        assertEquals(800 - 1000, sortable.getSortableRank());
    }

    @Test
    public void getSortableId() throws URISyntaxException {
        final var sortable = new ResourceTreeSortable<Topic>("resource", "topic", 100, 10, 1000);
        assertEquals(new URI("urn:resource:100"), sortable.getSortableId());
    }

    @Test
    public void getSortableParentId() throws URISyntaxException {
        final var sortable = new ResourceTreeSortable<Topic>("resource", "topic", 100, 10, 1000);
        assertEquals(new URI("urn:topic:10"), sortable.getSortableParentId());
    }

    @Test
    public void getTopicResource() {
        var sortable = new ResourceTreeSortable<Topic>("resource", "topic", 100, 10, 1000);
        assertFalse(sortable.getResourceConnection().isPresent());

        final var topic = mock(Topic.class);
        final var resource = mock(Resource.class);
        final var topicResource = mock(TopicResource.class);

        when(topic.getId()).thenReturn(101);
        when(resource.getId()).thenReturn(102);

        when(topicResource.getParent()).thenReturn(Optional.of(topic));
        when(topicResource.getResource()).thenReturn(Optional.of(resource));

        when(topicResource.getRank()).thenReturn(200 - 10000);

        sortable = new ResourceTreeSortable<Topic>(topicResource);
        assertSame(topicResource, sortable.getResourceConnection().orElse(null));
    }

    @Test
    public void testPopulateFromTopicResource() throws URISyntaxException {
        final var topic = mock(Topic.class);
        final var resource = mock(Resource.class);
        final var topicResource = mock(TopicResource.class);

        when(topic.getId()).thenReturn(101);
        when(resource.getId()).thenReturn(102);

        when(topicResource.getParent()).thenReturn(Optional.of(topic));
        when(topicResource.getResource()).thenReturn(Optional.of(resource));

        when(topicResource.getRank()).thenReturn(200);

        final var sortable = new ResourceTreeSortable<Topic>(topicResource);

        assertEquals(200 - 10000, sortable.getSortableRank());
        assertEquals(new URI("urn:topic:101"), sortable.getSortableParentId());
        assertEquals(new URI("urn:resource:102"), sortable.getSortableId());
    }
}