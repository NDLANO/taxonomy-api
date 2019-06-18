package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.ResolvablePathEntity;
import no.ndla.taxonomy.domain.ResolvedPath;
import no.ndla.taxonomy.domain.ResolvedPathEntity;
import no.ndla.taxonomy.repositories.ResolvedPathEntityRepository;
import no.ndla.taxonomy.repositories.ResolvedPathRepository;
import no.ndla.taxonomy.repositories.UpdateableEntityViewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class PathUpdaterServiceImpl implements PathUpdaterService {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private UpdateableEntityViewRepository updateableEntityViewRepository;
    private UpdateableEntityService updateableEntityService;
    private ResolvedPathEntityRepository updatedEntityRepository;
    private ResolvedPathRepository resolvedPathRepository;

    PathUpdaterServiceImpl(UpdateableEntityViewRepository updateableEntityViewRepository,
                           ResolvedPathEntityRepository updatedEntityRepository,
                           UpdateableEntityService updateableEntityService,
                           ResolvedPathRepository resolvedPathRepository) {
        this.updateableEntityViewRepository = updateableEntityViewRepository;
        this.updateableEntityService = updateableEntityService;
        this.updatedEntityRepository = updatedEntityRepository;
        this.resolvedPathRepository = resolvedPathRepository;
    }

    private void triggerChildUpdate(ResolvablePathEntity entity) {
        entity.getChildren().forEach(child -> updatedEntityRepository.triggerUpdate(child.getType(), child.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResolvablePathEntity> getEntitiesToUpdate() {
        return updateableEntityViewRepository.findEntitiesToUpdate(PageRequest.of(0, 1000))
                .stream()
                .map(updateableEntityService::getUpdateableEntity)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateEntity(ResolvablePathEntity updateableEntity) {
        final var updatedEntity = updatedEntityRepository.findOneByEntityTypeAndEntityId(updateableEntity.getType(), updateableEntity.getId()).orElseGet(() -> {
            final var newUpdatedEntity = new ResolvedPathEntity();
            newUpdatedEntity.setEntityId(updateableEntity.getId());
            newUpdatedEntity.setEntityType(updateableEntity.getType());
            updatedEntityRepository.save(newUpdatedEntity);

            return newUpdatedEntity;
        });

        final var updateSerial = updatedEntity.getUpdate();

        final var currentResolvedPaths = resolvedPathRepository.getResolvedPathByUpdatedEntity(updatedEntity);

        // Create a table of hash values for each record to be used to compare against the generated result
        // If there is no difference there is no need to save anything, and it is not necessary to do any child updates
        // also.

        final var currentResolvedPathHashes = new HashSet<String>();

        currentResolvedPaths.forEach(resolvedPath -> {
            currentResolvedPathHashes.add(resolvedPath.getPath() + ":" + (resolvedPath.isPrimary() ? "public" : "") + ":" + resolvedPath.getParentPublicId());
        });

        final var generatedPaths = updateableEntity.generatePaths();
        var updated = false;

        for (var generatedPath : generatedPaths) {
            log.info(generatedPath.getPath());

            final var hashValue = generatedPath.getPath() + ":" + (generatedPath.isPrimary() ? "public" : "") + ":" + generatedPath.getParentId();

            if (!currentResolvedPathHashes.contains(hashValue)) {
                updated = true;
            } else {
                currentResolvedPathHashes.remove(hashValue);
            }
        }

        if (updated || currentResolvedPathHashes.size() > 0) {
            log.info("Paths changed, doing update and triggering child updates");

            resolvedPathRepository.deleteAll(resolvedPathRepository.getResolvedPathByUpdatedEntity(updatedEntity));
            resolvedPathRepository.flush();

            for (var generatedPath : generatedPaths) {
                final var resolvedPath = new ResolvedPath();
                resolvedPath.setPath(generatedPath.getPath());
                resolvedPath.setPublicId(updateableEntity.getPublicId());
                resolvedPath.setPrimary(generatedPath.isPrimary());
                resolvedPath.setUpdatedEntity(updatedEntity);
                generatedPath.getParentId().ifPresent(resolvedPath::setParentPublicId);

                resolvedPathRepository.save(resolvedPath);
            }

            // Trigger update recursively downwards the tree structure
            triggerChildUpdate(updateableEntity);
        }

        updatedEntity.setUrlMapUpdatedAt(updateableEntity.getUpdatedAt());
        updatedEntityRepository.save(updatedEntity);

        if (updateSerial > 0) {
            updatedEntityRepository.saveUpdate(updatedEntity, updateSerial);
        }
    }
}
