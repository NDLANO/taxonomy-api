package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Subject;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.repositories.TopicSubtopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/*
    This class replicates old structure previously implemented as recursive queries in database, methods
    generally just returns flat lists that replicates the old database views
 */
@Transactional(propagation = Propagation.MANDATORY)
@Service
public class RecursiveTopicTreeServiceImpl implements RecursiveTopicTreeService {
    private final TopicSubtopicRepository topicSubtopicRepository;

    public RecursiveTopicTreeServiceImpl(TopicSubtopicRepository topicSubtopicRepository) {
        this.topicSubtopicRepository = topicSubtopicRepository;
    }

    private void addSubTopicIdsRecursively(Set<TopicTreeElement> topics, Set<Integer> topicIds, int ttl) {
        // Method just takes the list of topicIds provided and add each of the subtopics it finds to the list,
        // and then recursively runs the same method on each of the found subtopic IDs, once for each level

        if (--ttl < 0) {
            throw new IllegalStateException("Recursion limit reached, probably an infinite loop in the topic structure");
        }

        final var topicIdsThisLevel = new HashSet<Integer>();

        topicSubtopicRepository.findAllByTopicIdInIncludingTopicAndSubtopic(topicIds)
                .forEach(topicSubtopic -> {
                    topics.add(new TopicTreeElement(topicSubtopic.getSubtopicId(), topicSubtopic.getTopicId(), topicSubtopic.getTopicPublicId(), topicSubtopic.getRank()));
                    topicIdsThisLevel.add(topicSubtopic.getSubtopicId());
                });

        if (topicIdsThisLevel.size() > 0) {
            addSubTopicIdsRecursively(topics, topicIdsThisLevel, ttl);
        }
    }

    @Override
    public Set<TopicTreeElement> getRecursiveTopics(Topic topic) {
        final var toReturn = new HashSet<TopicTreeElement>();
        final var publicId = topic.getPublicId();
        if (publicId == null || !publicId.toString().startsWith("urn:subject:")) {
            toReturn.add(new TopicTreeElement(topic.getId(), null, null, 0));
        }

        addSubTopicIdsRecursively(toReturn, Set.of(topic.getId()), 1000);

        return toReturn;
    }
}
