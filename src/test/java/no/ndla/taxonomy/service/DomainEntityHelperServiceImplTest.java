package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.repositories.TopicRepository;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@Transactional
class DomainEntityHelperServiceImplTest {
    private Topic subject1;
    private Topic subject2;
    private Topic topic1;
    private Topic topic2;

    private DomainEntityHelperServiceImpl service;

    @BeforeEach
    void setUp(@Autowired TopicRepository topicRepository) {
        service = new DomainEntityHelperServiceImpl(topicRepository);

        topic1 = new Topic();
        topic1.setPublicId(URI.create("urn:topic:dehsit:1"));
        topic1 = topicRepository.save(topic1);

        topic2 = new Topic();
        topic2.setPublicId(URI.create("urn:topic:dehsit:2"));
        topic2 = topicRepository.save(topic2);

        subject1 = new Topic();
        subject1.setPublicId(URI.create("urn:subject:dehsit:1"));
        subject1 = topicRepository.save(subject1);

        subject2 = new Topic();
        subject2.setPublicId(URI.create("urn:subject:dehsit:2"));
        subject2 = topicRepository.save(subject2);
    }

    @Test
    void getSubjectByPublicId() {
        assertSame(subject1, service.getSubjectByPublicId(URI.create("urn:subject:dehsit:1")));
        assertSame(subject2, service.getSubjectByPublicId(URI.create("urn:subject:dehsit:2")));

        assertThrows(NotFoundServiceException.class, () -> service.getSubjectByPublicId(URI.create("urn:topic:dehsit:3")));
    }

    @Test
    void getTopicByPublicId() {
        assertSame(topic1, service.getTopicByPublicId(URI.create("urn:topic:dehsit:1")));
        assertSame(topic2, service.getTopicByPublicId(URI.create("urn:topic:dehsit:2")));

        assertThrows(NotFoundServiceException.class, () -> service.getTopicByPublicId(URI.create("urn:topic:dehsit:3")));
    }
}