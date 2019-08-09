package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.PathAlias;
import no.ndla.taxonomy.domain.Resource;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.repositories.PathAliasRepository;
import no.ndla.taxonomy.repositories.ResourceRepository;
import no.ndla.taxonomy.repositories.TopicRepository;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class PathAliasServiceTest {
    private RootPathService rootPathService;
    private TopicRepository topicRepository;
    private ResourceRepository resourceRepository;
    private PathAliasRepository pathAliasRepository;
    private PathAliasService pathAliasService;

    @Before
    public void beforeTesting() {
        rootPathService = mock(RootPathService.class);
        topicRepository = mock(TopicRepository.class);
        resourceRepository = mock(ResourceRepository.class);
        pathAliasRepository = mock(PathAliasRepository.class);
        pathAliasService = new PathAliasService(rootPathService, topicRepository, resourceRepository, pathAliasRepository);
    }

    @Test
    public void testInvalidPath() {
        assertFalse(pathAliasService.pathAliasForPath("").isPresent());
        assertFalse(pathAliasService.pathAliasForPath("//").isPresent());
    }

    @Test
    public void testWithOneItem() throws URISyntaxException {
        given(pathAliasRepository.findAllByOriginalPath("topic:1")).willReturn(Collections.emptyList());
        given(rootPathService.generateRootPath(new URI("urn:topic:1"))).willReturn(Optional.of("fag/1"));
        given(pathAliasRepository.findByAlias(anyString())).willReturn(Optional.empty());
        given(pathAliasRepository.save(any(PathAlias.class))).willAnswer(inv -> inv.<PathAlias>getArgument(0));
        var pathAlias = pathAliasService.pathAliasForPath("topic:1").orElse(null);
        assertNotNull(pathAlias);
        assertTrue(pathAlias.getAlias().startsWith("fag/1-0."));
    }

    @Test
    public void testWithOneItemAndDuplicate() throws URISyntaxException {
        given(pathAliasRepository.findAllByOriginalPath("topic:1")).willReturn(Collections.emptyList());
        given(rootPathService.generateRootPath(new URI("urn:topic:1"))).willReturn(Optional.of("fag/1"));
        var scope = new Object() {
            public int counter = 0;
        };
        var dupAlias = mock(PathAlias.class);
        given(pathAliasRepository.findByAlias(anyString())).willAnswer(inv -> scope.counter++ != 0 ? Optional.empty() : Optional.of(dupAlias));
        given(pathAliasRepository.save(any(PathAlias.class))).willAnswer(inv -> inv.<PathAlias>getArgument(0));
        var pathAlias = pathAliasService.pathAliasForPath("topic:1").orElse(null);
        assertNotNull(pathAlias);
        assertTrue(pathAlias.getAlias().startsWith("fag/1-1."));
    }

    @Test
    public void testWithTwoItems() throws URISyntaxException {
        given(pathAliasRepository.findAllByOriginalPath("subject:1/topic:1")).willReturn(Collections.emptyList());
        given(rootPathService.generateRootPath(new URI("urn:subject:1"))).willReturn(Optional.of("fag/1"));
        given(resourceRepository.findByPublicId(new URI("urn:subject:1"))).willReturn(null);
        var topic = mock(Topic.class);
        given(topic.getName()).willReturn("Helsefagarbeiderens rolle i ernæringsarbeidet");
        given(topicRepository.findByPublicId(new URI("urn:topic:1"))).willReturn(topic);
        given(pathAliasRepository.findByAlias(anyString())).willReturn(Optional.empty());
        given(pathAliasRepository.save(any(PathAlias.class))).willAnswer(inv -> inv.<PathAlias>getArgument(0));
        var pathAlias = pathAliasService.pathAliasForPath("subject:1/topic:1").orElse(null);
        assertNotNull(pathAlias);
        assertTrue(pathAlias.getAlias().startsWith("fag/1/helsefagarbeiderens-rolle-ernringsarbeidet-0."));
    }

    @Test
    public void testWithThreeItems() throws URISyntaxException {
        given(pathAliasRepository.findAllByOriginalPath("subject:1/topic:1/resource:1")).willReturn(Collections.emptyList());
        given(rootPathService.generateRootPath(new URI("urn:subject:1"))).willReturn(Optional.of("fag/1"));
        given(resourceRepository.findByPublicId(new URI("urn:subject:1"))).willReturn(null);
        var resource = mock(Resource.class);
        given(resource.getName()).willReturn("Helsefagarbeiderens rolle i ernæringsarbeidet");
        given(resourceRepository.findByPublicId(new URI("urn:resource:1"))).willReturn(resource);
        given(topicRepository.findByPublicId(new URI("urn:resource:1"))).willReturn(null);
        given(pathAliasRepository.findByAlias(anyString())).willReturn(Optional.empty());
        given(pathAliasRepository.save(any(PathAlias.class))).willAnswer(inv -> inv.<PathAlias>getArgument(0));
        var pathAlias = pathAliasService.pathAliasForPath("subject:1/topic:1/resource:1").orElse(null);
        assertNotNull(pathAlias);
        assertTrue(pathAlias.getAlias().startsWith("fag/1/helsefagarbeiderens-rolle-ernringsarbeidet-0."));
    }

    @Test
    public void testWithExistingAlias() {
        {
            var pathAlias = mock(PathAlias.class);
            given(pathAlias.getAlias()).willReturn("path/alias");
            given(pathAliasRepository.findAllByOriginalPath("subject:1/topic:1/resource:1")).willReturn(List.of(pathAlias));
        }
        var pathAlias = pathAliasService.pathAliasForPath("subject:1/topic:1/resource:1").orElse(null);
        assertNotNull(pathAlias);
        assertTrue(pathAlias.getAlias().startsWith("path/alias"));
    }
}
