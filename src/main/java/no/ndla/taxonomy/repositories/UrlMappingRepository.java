package no.ndla.taxonomy.repositories;

import no.ndla.taxonomy.domain.UrlMapping;
import org.springframework.data.repository.CrudRepository;

public interface UrlMappingRepository extends CrudRepository<UrlMapping, String> {
}
