package no.ndla.taxonomy.repositories;

import no.ndla.taxonomy.domain.ResolvedPathEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface ResolvedPathEntityRepository extends JpaRepository<ResolvedPathEntity, UUID> {
    Optional<ResolvedPathEntity> findOneByEntityTypeAndEntityId(String entityType, int entityId);

    @Modifying
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query("UPDATE ResolvedPathEntity ue SET ue.update = ue.update + 1 WHERE ue.entityType = :entityType AND ue.entityId = :entityId")
    void triggerUpdate(String entityType, Integer entityId);

    @Modifying
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query("UPDATE ResolvedPathEntity ue SET ue.update = ue.update - :updateSerial WHERE ue = :updatedEntity AND ue.update >= :updateSerial")
    void saveUpdate(ResolvedPathEntity updatedEntity, int updateSerial);
}
