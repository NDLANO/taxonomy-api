package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Resource;
import no.ndla.taxonomy.domain.Subject;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.repositories.ResourceRepository;
import no.ndla.taxonomy.repositories.SubjectRepository;
import no.ndla.taxonomy.repositories.TopicRepository;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class NorwegianRootPathServiceTest {
    private SubjectRepository subjectRepository;
    private TopicRepository topicRepository;
    private ResourceRepository resourceRepository;
    private NorwegianRootPathService norwegianRootPathService;

    @Before
    public void beforeTesting() {
        subjectRepository = mock(SubjectRepository.class);
        topicRepository = mock(TopicRepository.class);
        resourceRepository = mock(ResourceRepository.class);
        norwegianRootPathService = new NorwegianRootPathService(subjectRepository, topicRepository, resourceRepository);
    }

    @Test
    public void testNotFound() throws URISyntaxException {
        var uri = new URI("subject:1");
        given(subjectRepository.findByPublicId(uri)).willReturn(null);
        given(topicRepository.findByPublicId(uri)).willReturn(null);
        given(resourceRepository.findByPublicId(uri)).willReturn(null);
        assertFalse(norwegianRootPathService.generateRootPath(uri).isPresent());
    }

    @Test
    public void testSubject() throws URISyntaxException {
        var uri = new URI("subject:1");
        var subject = mock(Subject.class);
        given(subject.getName()).willReturn("Helsearbeiderfag");
        given(subjectRepository.findByPublicId(uri)).willReturn(subject);
        given(topicRepository.findByPublicId(uri)).willReturn(null);
        given(resourceRepository.findByPublicId(uri)).willReturn(null);
        assertEquals("fag/helsearbeiderfag", norwegianRootPathService.generateRootPath(uri).orElse(null));
    }

    @Test
    public void testTopic() throws URISyntaxException {
        var uri = new URI("topic:1");
        given(subjectRepository.findByPublicId(uri)).willReturn(null);
        var topic = mock(Topic.class);
        given(topic.getName()).willReturn("Helsefagarbeiderens rolle i ernæringsarbeidet");
        given(topicRepository.findByPublicId(uri)).willReturn(topic);
        given(resourceRepository.findByPublicId(uri)).willReturn(null);
        assertEquals("emne/helsefagarbeiderens-rolle-ernringsarbeidet", norwegianRootPathService.generateRootPath(uri).orElse(null));
    }

    @Test
    public void testResource() throws URISyntaxException {
        var uri = new URI("topic:1");
        given(subjectRepository.findByPublicId(uri)).willReturn(null);
        given(topicRepository.findByPublicId(uri)).willReturn(null);
        var resource = mock(Resource.class);
        given(resource.getName()).willReturn("Helsefagarbeiderens rolle i ernæringsarbeidet");
        given(resourceRepository.findByPublicId(uri)).willReturn(resource);
        assertEquals("ressurs/helsefagarbeiderens-rolle-ernringsarbeidet", norwegianRootPathService.generateRootPath(uri).orElse(null));
    }
}
