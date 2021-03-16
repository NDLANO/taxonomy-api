package no.ndla.taxonomy.service;

import no.ndla.taxonomy.TestSeeder;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.domain.TopicSubtopic;
import no.ndla.taxonomy.repositories.TopicRepository;
import no.ndla.taxonomy.repositories.TopicSubtopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@Transactional
class RecursiveTopicTreeServiceImplTest {
    private TopicRepository topicRepository;
    private TopicSubtopicRepository topicSubtopicRepository;

    private RecursiveTopicTreeServiceImpl service;

    @BeforeEach
    void setUp(@Autowired TopicSubtopicRepository topicSubtopicRepository,
               @Autowired TopicRepository topicRepository,
               @Autowired TestSeeder testSeeder) {
        this.topicRepository = topicRepository;
        this.topicSubtopicRepository = topicSubtopicRepository;

        topicRepository.deleteAllAndFlush();

        testSeeder.recursiveTopicsBySubjectIdTestSetup();

        service = new RecursiveTopicTreeServiceImpl(topicSubtopicRepository);
    }

    @Test
    void getRecursiveTopics_by_subject() {
        final var subject = topicRepository.findFirstByPublicId(URI.create("urn:subject:1")).orElseThrow();
        final var topicElements = service.getRecursiveTopics(subject);

        final var topicsToFind = new HashSet<>(Set.of("urn:topic:1", "urn:topic:2", "urn:topic:3", "urn:topic:4", "urn:topic:5", "urn:topic:6", "urn:topic:7", "urn:topic:8"));

        final var topic1 = topicRepository.findFirstByPublicId(URI.create("urn:topic:1")).orElseThrow();
        final var topic3 = topicRepository.findFirstByPublicId(URI.create("urn:topic:3")).orElseThrow();
        final var topic5 = topicRepository.findFirstByPublicId(URI.create("urn:topic:5")).orElseThrow();

        topicElements.forEach(topicTreeElement -> {
            final var topic = topicRepository.findById(topicTreeElement.getTopicId()).orElseThrow();

            if (!topicsToFind.contains(topic.getPublicId().toString())) {
                fail("Topic found is unknown or duplicated " + topic.getPublicId());
            }

            switch (topic.getPublicId().toString()) {
                case "urn:topic:1":
                    // Child if subject
                    assertEquals(subject.getId(), topicTreeElement.getParentSubjectId().orElseThrow());
                    assertFalse(topicTreeElement.getParentTopicId().isPresent());
                    assertEquals(1, topicTreeElement.getRank());
                    break;
                case "urn:topic:2":
                    // Child of topic 1
                    assertFalse(topicTreeElement.getParentSubjectId().isPresent());
                    assertEquals(topic1.getId(), topicTreeElement.getParentTopicId().orElseThrow());
                    assertEquals(1, topicTreeElement.getRank());
                    break;
                case "urn:topic:3":
                    // Child if subject
                    assertEquals(subject.getId(), topicTreeElement.getParentSubjectId().orElseThrow());
                    assertFalse(topicTreeElement.getParentTopicId().isPresent());
                    assertEquals(2, topicTreeElement.getRank());
                    break;
                case "urn:topic:4":
                    // Child of topic 3
                    assertFalse(topicTreeElement.getParentSubjectId().isPresent());
                    assertEquals(topic3.getId(), topicTreeElement.getParentTopicId().orElseThrow());
                    assertEquals(1, topicTreeElement.getRank());
                    break;
                case "urn:topic:5":
                    // Child if subject
                    assertEquals(subject.getId(), topicTreeElement.getParentSubjectId().orElseThrow());
                    assertFalse(topicTreeElement.getParentTopicId().isPresent());
                    assertEquals(3, topicTreeElement.getRank());
                    break;
                case "urn:topic:6":
                    // Child of topic 5
                    assertFalse(topicTreeElement.getParentSubjectId().isPresent());
                    assertEquals(topic5.getId(), topicTreeElement.getParentTopicId().orElseThrow());
                    assertEquals(1, topicTreeElement.getRank());
                    break;
                case "urn:topic:7":
                    // Child of topic 5
                    assertFalse(topicTreeElement.getParentSubjectId().isPresent());
                    assertEquals(topic5.getId(), topicTreeElement.getParentTopicId().orElseThrow());
                    assertEquals(2, topicTreeElement.getRank());
                    break;
                case "urn:topic:8":
                    // Child of topic 5
                    assertFalse(topicTreeElement.getParentSubjectId().isPresent());
                    assertEquals(topic5.getId(), topicTreeElement.getParentTopicId().orElseThrow());
                    assertEquals(3, topicTreeElement.getRank());
                    break;
                default:
                    fail();
            }

            topicsToFind.remove(topic.getPublicId().toString());
        });

        assertEquals(0, topicsToFind.size());
    }

    @Test
    void getRecursiveTopics_by_topic() {

        final var topic1 = topicRepository.findFirstByPublicId(URI.create("urn:topic:1")).orElseThrow();
        final var topic3 = topicRepository.findFirstByPublicId(URI.create("urn:topic:3")).orElseThrow();
        final var topic5 = topicRepository.findFirstByPublicId(URI.create("urn:topic:5")).orElseThrow();

        // Search for subtopics of topic1
        {
            final var topicElements = service.getRecursiveTopics(topic1);
            final var topicsToFind = new HashSet<>(Set.of("urn:topic:1", "urn:topic:2"));

            topicElements.forEach(topicTreeElement -> {
                final var topic = topicRepository.findById(topicTreeElement.getTopicId()).orElseThrow();

                if (!topicsToFind.contains(topic.getPublicId().toString())) {
                    fail("Topic found is unknown or duplicated " + topic.getPublicId());
                }

                switch (topic.getPublicId().toString()) {
                    case "urn:topic:1":
                        assertFalse(topicTreeElement.getParentTopicId().isPresent());
                        assertEquals(0, topicTreeElement.getRank());
                        break;
                    case "urn:topic:2":
                        assertFalse(topicTreeElement.getParentSubjectId().isPresent());
                        assertEquals(topic1.getId(), topicTreeElement.getParentTopicId().orElseThrow());
                        assertEquals(1, topicTreeElement.getRank());
                        break;
                    default:
                        fail();
                }

                topicsToFind.remove(topic.getPublicId().toString());
            });

            assertEquals(0, topicsToFind.size());
        }

        // Search for subtopics of topic3
        {
            final var topicElements = service.getRecursiveTopics(topic3);
            final var topicsToFind = new HashSet<>(Set.of("urn:topic:3", "urn:topic:4"));

            topicElements.forEach(topicTreeElement -> {
                final var topic = topicRepository.findById(topicTreeElement.getTopicId()).orElseThrow();

                if (!topicsToFind.contains(topic.getPublicId().toString())) {
                    fail("Topic found is unknown or duplicated " + topic.getPublicId());
                }

                switch (topic.getPublicId().toString()) {
                    case "urn:topic:3":
                        assertFalse(topicTreeElement.getParentTopicId().isPresent());
                        assertEquals(0, topicTreeElement.getRank());
                        break;
                    case "urn:topic:4":
                        assertFalse(topicTreeElement.getParentSubjectId().isPresent());
                        assertEquals(topic3.getId(), topicTreeElement.getParentTopicId().orElseThrow());
                        assertEquals(1, topicTreeElement.getRank());
                        break;
                    default:
                        fail();
                }

                topicsToFind.remove(topic.getPublicId().toString());
            });

            assertEquals(0, topicsToFind.size());
        }

        // Search for subtopics of topic5
        {
            final var topicElements = service.getRecursiveTopics(topic5);
            final var topicsToFind = new HashSet<>(Set.of("urn:topic:5", "urn:topic:6", "urn:topic:7", "urn:topic:8"));

            topicElements.forEach(topicTreeElement -> {
                final var topic = topicRepository.findById(topicTreeElement.getTopicId()).orElseThrow();

                if (!topicsToFind.contains(topic.getPublicId().toString())) {
                    fail("Topic found is unknown or duplicated " + topic.getPublicId());
                }

                switch (topic.getPublicId().toString()) {
                    case "urn:topic:5":
                        assertFalse(topicTreeElement.getParentTopicId().isPresent());
                        assertEquals(0, topicTreeElement.getRank());
                        break;
                    case "urn:topic:6":
                        assertFalse(topicTreeElement.getParentSubjectId().isPresent());
                        assertEquals(topic5.getId(), topicTreeElement.getParentTopicId().orElseThrow());
                        assertEquals(1, topicTreeElement.getRank());
                        break;
                    case "urn:topic:7":
                        assertFalse(topicTreeElement.getParentSubjectId().isPresent());
                        assertEquals(topic5.getId(), topicTreeElement.getParentTopicId().orElseThrow());
                        assertEquals(2, topicTreeElement.getRank());
                        break;
                    case "urn:topic:8":
                        assertFalse(topicTreeElement.getParentSubjectId().isPresent());
                        assertEquals(topic5.getId(), topicTreeElement.getParentTopicId().orElseThrow());
                        assertEquals(3, topicTreeElement.getRank());
                        break;
                    default:
                        fail();
                }

                topicsToFind.remove(topic.getPublicId().toString());
            });

            assertEquals(0, topicsToFind.size());
        }
    }

    @Test
    void getRecursiveTopics_with_infinite_loop() {
        // This condition should not be possible as validation when inserting should prevent it, this tests
        // tests that the breaker works in case it happens, for example by manual editing. Otherwise it would
        // result in a StackOverflowError at some point

        final var topic1 = new Topic();
        final var topic2 = new Topic();
        final var topic3 = new Topic();
        final var topic4 = new Topic();

        topicRepository.saveAll(Set.of(topic1, topic2, topic3, topic4));

        // Link it in the order of
        // → topic 1 → topic 2 ↓
        // ↑ topic 4 ← topic 3 ←

        topicSubtopicRepository.saveAll(Set.of(
                TopicSubtopic.create(topic1, topic2),
                TopicSubtopic.create(topic2, topic3),
                TopicSubtopic.create(topic3, topic4),
                TopicSubtopic.create(topic4, topic1)
        ));

        assertThrows(IllegalStateException.class, () -> service.getRecursiveTopics(topic1));
        assertThrows(IllegalStateException.class, () -> service.getRecursiveTopics(topic2));
        assertThrows(IllegalStateException.class, () -> service.getRecursiveTopics(topic3));
        assertThrows(IllegalStateException.class, () -> service.getRecursiveTopics(topic4));
    }
}