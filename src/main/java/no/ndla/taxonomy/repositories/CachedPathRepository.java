package no.ndla.taxonomy.repositories;

import no.ndla.taxonomy.domain.CachedPath;
import org.springframework.data.jpa.repository.JpaRepository;

import java.net.URI;
import java.util.List;

public interface CachedPathRepository extends JpaRepository<CachedPath, String> {
    List<CachedPath> findAllByPublicId(URI publicId);
}
