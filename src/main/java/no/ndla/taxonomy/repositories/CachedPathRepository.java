package no.ndla.taxonomy.repositories;

import no.ndla.taxonomy.domain.CachedPath;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import javax.persistence.LockModeType;
import java.net.URI;
import java.util.List;

public interface CachedPathRepository extends JpaRepository<CachedPath, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<CachedPath> findAllByPublicId(URI publicId);
}
