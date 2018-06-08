package no.ndla.taxonomy.service.repositories;

import no.ndla.taxonomy.domain.SubjectTopic;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.domain.TopicResource;
import no.ndla.taxonomy.domain.TopicSubtopic;

import javax.persistence.EntityManager;

public class TopicRepositoryImpl implements TaxonomyRepositoryCustom<Topic> {

    private EntityManager entityManager;

    public TopicRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void delete(Topic topic) {
        for (TopicSubtopic edge : topic.parentTopics.toArray(new TopicSubtopic[]{})) {
            edge.getTopic().removeSubtopic(topic);
        }
        for (TopicSubtopic edge : topic.subtopics.toArray(new TopicSubtopic[]{})) {
            topic.removeSubtopic(edge.getSubtopic());
        }
        for (SubjectTopic edge : topic.subjects.toArray(new SubjectTopic[]{})) {
            edge.getSubject().removeTopic(topic);
        }
        for (TopicResource edge : topic.resources.toArray(new TopicResource[]{})) {
            topic.removeResource(edge.getResource());
        }

        entityManager.remove(topic);
    }
}
