package no.ndla.taxonomy.service.repositories;

import no.ndla.taxonomy.service.domain.Subject;

import javax.persistence.EntityManager;

public class SubjectRepositoryImpl implements TaxonomyRepositoryCustom<Subject> {
    private EntityManager entityManager;

    public SubjectRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void delete(Subject subject) {
        subject.getTopics().forEachRemaining(t -> subject.removeTopic(t));
        entityManager.remove(subject);
    }
}
