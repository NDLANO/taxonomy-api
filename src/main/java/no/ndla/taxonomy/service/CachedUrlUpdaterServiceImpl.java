/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.CachedPath;
import no.ndla.taxonomy.domain.EntityWithPath;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.repositories.NodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CachedUrlUpdaterServiceImpl implements CachedUrlUpdaterService<Node> {
    private final NodeRepository nodeRepository;

    public CachedUrlUpdaterServiceImpl(NodeRepository nodeRepository) {
        this.nodeRepository = nodeRepository;
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
    public void updateCachedUrls(Node entity) {
        Set.copyOf(entity.getChildConnections())
                .forEach(childEntity -> childEntity.getConnectedChild().ifPresent(e -> updateCachedUrls((Node) e)));

        clearCachedUrls(entity);

        final var newPathsToEntity = createPathsToEntity(entity);
        final var newCachedPathObjects = newPathsToEntity.stream().map(newPath -> {
            final var cachedPath = new CachedPath();
            cachedPath.setPath(newPath.path);
            cachedPath.setPrimary(newPath.isPrimary);
            cachedPath.setOwningEntity(entity);

            return cachedPath;
        }).collect(Collectors.toSet());

        entity.setCachedPaths(newCachedPathObjects);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void clearCachedUrls(Node entity) {
        entity.setCachedPaths(new HashSet<>());
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
