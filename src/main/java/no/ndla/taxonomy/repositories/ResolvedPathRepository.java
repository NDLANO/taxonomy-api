package no.ndla.taxonomy.repositories;

import no.ndla.taxonomy.domain.ResolvedPath;
import no.ndla.taxonomy.domain.ResolvedPathEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResolvedPathRepository extends JpaRepository<ResolvedPath, UUID> {
    List<ResolvedPath> getResolvedPathByUpdatedEntity(ResolvedPathEntity updatedEntity);

    List<ResolvedPath> getAllByPublicId(URI publicId);

    Optional<ResolvedPath> getFirstByPublicIdAndIsPrimary(URI publicId, boolean isPrimary);
}
