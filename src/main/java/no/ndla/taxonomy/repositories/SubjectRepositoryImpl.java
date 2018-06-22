package no.ndla.taxonomy.repositories;

import no.ndla.taxonomy.domain.Subject;
import no.ndla.taxonomy.domain.SubjectTopic;
import no.ndla.taxonomy.domain.Topic;

import javax.persistence.EntityManager;

public class SubjectRepositoryImpl implements TaxonomyRepositoryCustom<Subject> {
    private EntityManager entityManager;

    public SubjectRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void delete(Subject subject) {
        final SubjectTopic[] topics = subject.topics.toArray(new SubjectTopic[]{});
        for (SubjectTopic topicSubtopic : topics) {
            Topic topic = topicSubtopic.getTopic();
            subject.removeTopic(topic);
        }
        entityManager.remove(subject);
    }
}
