/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.CachedPath;
import no.ndla.taxonomy.domain.EntityWithPath;
import no.ndla.taxonomy.repositories.CachedPathRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CachedUrlUpdaterServiceImpl implements CachedUrlUpdaterService {
    private final CachedPathRepository cachedPathRepository;

    public CachedUrlUpdaterServiceImpl(CachedPathRepository cachedPathRepository) {
        this.cachedPathRepository = cachedPathRepository;
    }

    private Set<PathToEntity> createPathsToEntity(EntityWithPath entity) {
        final var returnedPaths = new HashSet<PathToEntity>();

        // This entity can be root path
        if (entity.isContext()) {
            returnedPaths.add(new PathToEntity("/" + entity.getPublicId().getSchemeSpecificPart(), true));
        }

        // Get all parent paths, append this entity publicId to the end of the actual path and add
        // all to the list to return
        entity.getParentConnections()
                .forEach(parentConnection -> parentConnection.getConnectedParent().ifPresent(parent -> {
                    createPathsToEntity(parent).stream()
                            .map(parentPath -> new PathToEntity(
                                    parentPath.path + "/" + entity.getPublicId().getSchemeSpecificPart(),
                                    parentConnection.isPrimary().orElse(true)))
                            .forEach(returnedPaths::add);
                }));

        return returnedPaths;
    }

    /*
     * Method recursively re-creates all CachedPath entries for the entity by removing old entities and creating new
     * ones
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void updateCachedUrls(EntityWithPath entity) {
        Set.copyOf(entity.getChildConnections())
                .forEach(childEntity -> childEntity.getConnectedChild().ifPresent(this::updateCachedUrls));

        clearCachedUrls(entity);

        final var newPathsToEntity = createPathsToEntity(entity);
        final var newCachedPathObjects = newPathsToEntity.stream().map(newPath -> {
            final var cachedPath = new CachedPath();
            cachedPath.setPath(newPath.path);
            cachedPath.setPrimary(newPath.isPrimary);
            cachedPath.setOwningEntity(entity);
            cachedPath.setActive(true);

            return cachedPath;
        }).collect(Collectors.toSet());

        cachedPathRepository.saveAll(newCachedPathObjects);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void clearCachedUrls(EntityWithPath entity) {
        Set.copyOf(entity.getCachedPaths()).forEach(CachedPath::disable);
    }

    private static class PathToEntity {
        private final boolean isPrimary;
        private final String path;

        private PathToEntity(String path, boolean isPrimary) {
            this.path = path;
            this.isPrimary = isPrimary;
        }
    }
}
