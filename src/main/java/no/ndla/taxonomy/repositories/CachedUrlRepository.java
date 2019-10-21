package no.ndla.taxonomy.repositories;

import no.ndla.taxonomy.domain.CachedUrl;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CachedUrlRepository extends JpaRepository<CachedUrl, String> {
}
