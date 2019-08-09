package no.ndla.taxonomy.repositories;

import no.ndla.taxonomy.domain.PathAlias;
import org.springframework.data.repository.CrudRepository;

import java.util.Collection;
import java.util.Optional;

public interface PathAliasRepository extends CrudRepository<PathAlias, Integer> {
    Optional<PathAlias> findByAlias(String alias);
    Collection<PathAlias> findAllByOriginalPath(String originalPath);
}
