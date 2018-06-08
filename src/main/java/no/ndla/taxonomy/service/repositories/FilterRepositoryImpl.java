package no.ndla.taxonomy.service.repositories;

import no.ndla.taxonomy.domain.Filter;
import no.ndla.taxonomy.domain.ResourceFilter;
import no.ndla.taxonomy.domain.TopicFilter;

import javax.persistence.EntityManager;

public class FilterRepositoryImpl implements TaxonomyRepositoryCustom<Filter> {
    private EntityManager entityManager;

    public FilterRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void delete(Filter filter) {
        for (ResourceFilter edge : filter.resources.toArray(new ResourceFilter[]{})) {
            edge.getResource().removeFilter(filter);
        }
        for (TopicFilter edge : filter.topics.toArray(new TopicFilter[]{})) {
            edge.getTopic().removeFilter(filter);
        }
        entityManager.remove(filter);
    }
}
