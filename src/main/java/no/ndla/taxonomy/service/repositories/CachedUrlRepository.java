package no.ndla.taxonomy.service.repositories;

import no.ndla.taxonomy.service.domain.CachedUrl;
import org.springframework.data.repository.CrudRepository;

import java.net.URI;
import java.util.Collection;

public interface CachedUrlRepository extends CrudRepository<CachedUrl, Integer> {
    Collection<CachedUrl> findByPublicId(URI id);
    void deleteByPublicId(URI id);

    void deleteByPathStartingWith(String path);
}
