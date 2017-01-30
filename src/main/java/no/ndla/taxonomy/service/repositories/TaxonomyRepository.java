package no.ndla.taxonomy.service.repositories;

import no.ndla.taxonomy.service.domain.NotFoundException;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.net.URI;

@NoRepositoryBean
public interface TaxonomyRepository<T> extends CrudRepository<T, Integer> {
    T findByPublicId(URI id);

    default T getByPublicId(URI id) {
        T entity = findByPublicId(id);
        if (null == entity) throw new NotFoundException("entity", id);
        return entity;
    }

    void deleteByPublicId(URI id);
}
