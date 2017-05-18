package no.ndla.taxonomy.service.repositories;

import no.ndla.taxonomy.service.domain.CachedUrl;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;

public class CachedUrlRepositoryImpl implements CachedUrlRepositoryCustom {

    private EntityManager entityManager;

    public CachedUrlRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void truncate() {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaDelete<CachedUrl> query = builder.createCriteriaDelete(CachedUrl.class);
        query.from(CachedUrl.class);
        entityManager.createQuery(query).executeUpdate();
    }
}
