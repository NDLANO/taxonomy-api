package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Subject;
import no.ndla.taxonomy.domain.SubjectTopic;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.repositories.TopicSubtopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Transactional(propagation = Propagation.MANDATORY)
@Service
public class RecursiveTopicTreeServiceImpl implements RecursiveTopicTreeService {
    private final TopicSubtopicRepository topicSubtopicRepository;

    public RecursiveTopicTreeServiceImpl(TopicSubtopicRepository topicSubtopicRepository) {
        this.topicSubtopicRepository = topicSubtopicRepository;
    }

    private void addSubTopicIdsRecursively(Set<TopicTreeElement> topics, Set<Integer> topicIds) {
        final var topicIdsThisLevel = new HashSet<Integer>();

        topicSubtopicRepository.findAllByTopicIdInIncludingTopicAndSubtopic(topicIds)
                .forEach(topicSubtopic -> {
                    topics.add(new TopicTreeElement(topicSubtopic.getSubtopicId(), null, topicSubtopic.getTopicId(), topicSubtopic.getRank()));
                    topicIdsThisLevel.add(topicSubtopic.getSubtopicId());
                });

        if (topicIdsThisLevel.size() > 0) {
            addSubTopicIdsRecursively(topics, topicIdsThisLevel);
        }
    }

    @Override
    public Set<TopicTreeElement> getRecursiveTopics(Topic topic) {
        final var toReturn = new HashSet<TopicTreeElement>();
        toReturn.add(new TopicTreeElement(topic.getId(), null, null, 0));

        addSubTopicIdsRecursively(toReturn, Set.of(topic.getId()));

        return toReturn;
    }

    @Override
    public Set<TopicTreeElement> getRecursiveTopics(Subject subject) {
        final var toReturn = new HashSet<TopicTreeElement>();

        final var subjectTopics = subject.getSubjectTopics()
                .stream()
                .filter(st -> st.getTopic().isPresent())
                .filter(st -> st.getSubject().isPresent())
                .collect(Collectors.toSet());

        final var subtopicIds = subjectTopics.stream()
                .map(SubjectTopic::getTopic)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(Topic::getId)
                .collect(Collectors.toSet());

        subjectTopics.forEach(subjectTopic -> {
            final var topicId = subjectTopic.getTopic().orElseThrow().getId();
            final var subjectId = subjectTopic.getSubject().orElseThrow().getId();

            toReturn.add(new TopicTreeElement(topicId, subjectId, null, subjectTopic.getRank()));
        });

        addSubTopicIdsRecursively(toReturn, subtopicIds);

        return toReturn;
    }
}
