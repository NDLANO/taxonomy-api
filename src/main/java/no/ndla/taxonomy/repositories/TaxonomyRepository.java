package no.ndla.taxonomy.repositories;

import no.ndla.taxonomy.domain.NotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.net.URI;

@NoRepositoryBean
public interface TaxonomyRepository<T> extends JpaRepository<T, Integer> {
    T findByPublicId(URI id);

    default T getByPublicId(URI id) {
        T entity = findByPublicId(id);
        if (null == entity) throw new NotFoundException("entity", id);
        return entity;
    }

    void deleteByPublicId(URI id);
}
