/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.CachedPath;
import no.ndla.taxonomy.domain.Context;
import no.ndla.taxonomy.domain.LanguageField;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.util.HashUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CachedUrlUpdaterServiceImpl implements CachedUrlUpdaterService {
    private final NodeRepository nodeRepository;

    public CachedUrlUpdaterServiceImpl(NodeRepository nodeRepository) {
        this.nodeRepository = nodeRepository;
    }

    private Set<Context> createContexts(Node entity) {
        final var returnedContexts = new HashSet<Context>();

        // This entity can be root path
        if (entity.isContext()) {
            returnedContexts.add(new Context(entity.getPublicId().toString(), LanguageField.fromNode(entity),
                    "/" + entity.getPublicId().getSchemeSpecificPart(), new LanguageField<List<String>>(),
                    entity.getContextType(), Optional.empty(), entity.isVisible(), true, "urn:relevance:core",
                    HashUtil.semiHash(entity.getPublicId())));
        }

        // Get all parent connections, append this entity publicId to the end of the actual path and add
        // all to the list to return
        entity.getParentConnections().forEach(parentConnection -> parentConnection.getParent().ifPresent(parent -> {
            createContexts(parent).stream().map(parentContext -> {
                var breadcrumbs = LanguageField.listFromLists(parentContext.breadcrumbs(),
                        LanguageField.fromNode(parent));
                return new Context(parentContext.rootId(), parentContext.rootName(),
                        parentContext.path() + "/" + entity.getPublicId().getSchemeSpecificPart(), breadcrumbs,
                        entity.getContextType(), Optional.of(parent.getPublicId().toString()),
                        parentContext.isVisible() && entity.isVisible(), parentConnection.isPrimary().orElse(true),
                        parentConnection.getRelevance()
                                .flatMap(relevance -> Optional.of(relevance.getPublicId().toString()))
                                .orElse("urn:relevance:core"),
                        HashUtil.semiHash(parentContext.rootId() + parentConnection.getPublicId()));

            }).forEach(returnedContexts::add);
        }));

        return returnedContexts;
    }

    /*
     * Method recursively re-creates all CachedPath entries for the entity by removing old entities and creating new
     * ones
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void updateCachedUrls(Node entity) {
        Set.copyOf(entity.getChildConnections())
                .forEach(childEntity -> childEntity.getChild().ifPresent(this::updateCachedUrls));

        clearCachedUrls(entity);

        final var newPathsToEntity = createContexts(entity);
        final var newCachedPathObjects = newPathsToEntity.stream().map(newPath -> {
            final var cachedPath = new CachedPath();
            cachedPath.setPath(newPath.path());
            cachedPath.setPrimary(newPath.isPrimary());
            cachedPath.setNode(entity);

            return cachedPath;
        }).collect(Collectors.toSet());

        entity.setCachedPaths(newCachedPathObjects);
        entity.setContexts(newPathsToEntity);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void clearCachedUrls(Node entity) {
        entity.setCachedPaths(new HashSet<>());
        entity.setContexts(new HashSet<>());
    }

}
