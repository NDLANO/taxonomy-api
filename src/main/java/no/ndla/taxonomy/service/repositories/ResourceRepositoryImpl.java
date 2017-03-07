package no.ndla.taxonomy.service.repositories;

import no.ndla.taxonomy.service.domain.Resource;

import javax.persistence.EntityManager;

public class ResourceRepositoryImpl implements TaxonomyRepositoryCustom<Resource> {
    private EntityManager entityManager;

    public ResourceRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void delete(Resource resource) {
        resource.getTopics().forEachRemaining(topic -> topic.removeResource(resource));
        entityManager.remove(resource);
    }
}
