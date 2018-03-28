package no.ndla.taxonomy.service.repositories;

import no.ndla.taxonomy.service.domain.Resource;
import no.ndla.taxonomy.service.domain.TopicResource;

import javax.persistence.EntityManager;

public class ResourceRepositoryImpl implements TaxonomyRepositoryCustom<Resource> {
    private EntityManager entityManager;

    public ResourceRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void delete(Resource resource) {
        for (TopicResource edge : resource.topics.toArray(new TopicResource[]{})) {
            edge.getTopic().removeResource(resource);
        }
        entityManager.remove(resource);
    }
}
