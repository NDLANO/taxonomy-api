package no.ndla.taxonomy.repositories;

import no.ndla.taxonomy.domain.CachedUrl;
import org.springframework.data.jpa.repository.JpaRepository;

import java.net.URI;
import java.util.List;
import java.util.Optional;

public interface CachedUrlRepository extends JpaRepository<CachedUrl, String> {
    List<CachedUrl> findAllByPublicId(URI publicId);

    Optional<CachedUrl> findFirstByPublicIdAndPrimary(URI publicId, boolean primary);
}
