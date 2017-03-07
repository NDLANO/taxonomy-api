package no.ndla.taxonomy.service.repositories;

import no.ndla.taxonomy.service.domain.Topic;

import javax.persistence.EntityManager;

public class TopicRepositoryImpl implements TaxonomyRepositoryCustom<Topic> {

    private EntityManager entityManager;

    public TopicRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void delete(Topic topic) {
        topic.getParentTopics().forEachRemaining(parent -> parent.removeSubtopic(topic));
        topic.getSubtopics().forEachRemaining(subtopic -> topic.removeSubtopic(subtopic));
        topic.getSubjects().forEachRemaining(subject -> subject.removeTopic(topic));
        topic.getResources().forEachRemaining(resource -> topic.removeResource(resource));

        entityManager.remove(topic);
    }
}
