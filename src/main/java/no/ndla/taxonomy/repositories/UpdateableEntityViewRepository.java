package no.ndla.taxonomy.repositories;

import no.ndla.taxonomy.domain.ResolvablePathEntityView;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface UpdateableEntityViewRepository extends JpaRepository<ResolvablePathEntityView, UUID> {
    @Query("SELECT uev FROM ResolvablePathEntityView uev WHERE (uev.urlMapUpdatedAt IS NULL OR uev.urlMapUpdatedAt != uev.updatedAt OR uev.update > 0) ORDER BY uev.update ASC, urlMapUpdatedAt ASC")
    List<ResolvablePathEntityView> findEntitiesToUpdate(Pageable pageable);
}
