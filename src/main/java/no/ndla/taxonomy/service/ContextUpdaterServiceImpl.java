/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import java.util.*;
import no.ndla.taxonomy.config.Constants;
import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.util.HashUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContextUpdaterServiceImpl implements ContextUpdaterService {

    public ContextUpdaterServiceImpl() {}

    private Set<TaxonomyContext> createContexts(Node node) {
        final var returnedContexts = new HashSet<TaxonomyContext>();

        boolean activeContext = node.getCustomFields()
                .getOrDefault(Constants.SubjectCategory, Constants.Active)
                .matches(String.format("%s|%s|%s", Constants.Active, Constants.Beta, Constants.OtherResources));
        // This entity can be root path
        if (node.isContext()) {
            var contextId = HashUtil.semiHash(node.getPublicId());
            returnedContexts.add(new TaxonomyContext(
                    LanguageField.fromNode(node),
                    node.getPublicId().toString(),
                    LanguageField.fromNode(node),
                    node.getPathPart(),
                    new LanguageField<List<String>>(),
                    node.getContextType(),
                    new ArrayList<>(),
                    new ArrayList<>(),
                    node.isVisible(),
                    activeContext,
                    true,
                    Relevance.CORE.getPublicId().toString(),
                    contextId,
                    0,
                    "",
                    new ArrayList<>()));
        }

        // Get all parent connections, append all contexts from the parent to the list and return.
        node.getParentConnections()
                .forEach(parentConnection -> parentConnection.getParent().ifPresent(parent -> {
                    createContexts(parent).stream()
                            .map(parentContext -> {
                                var breadcrumbs = LanguageField.listFromLists(
                                        parentContext.breadcrumbs(), LanguageField.fromNode(parent));
                                var parentIds = parentContext.parentIds();
                                parentIds.add(parent.getPublicId().toString());
                                var parentContextIds = parentContext.parentContextIds();
                                parentContextIds.add(parentContext.contextId());
                                var contextId =
                                        HashUtil.mediumHash(parentContext.contextId() + parentConnection.getPublicId());
                                var parents = parentContext.parents();
                                parents.add(new TaxonomyCrumb(
                                        parent.getPublicId().toString(),
                                        parent.getNodeType(),
                                        parentContext.contextId(),
                                        parentContext.name(),
                                        parentContext.path()));
                                return new TaxonomyContext(
                                        LanguageField.fromNode(node),
                                        parentContext.rootId(),
                                        parentContext.rootName(),
                                        parentContext.path() + node.getPathPart(),
                                        breadcrumbs,
                                        node.getContextType(),
                                        parentIds,
                                        parentContextIds,
                                        parentContext.isVisible() && node.isVisible(),
                                        parentContext.isActive() && activeContext,
                                        parentConnection.isPrimary().orElse(false),
                                        parentConnection
                                                .getRelevance()
                                                .flatMap(relevance -> Optional.of(
                                                        relevance.getPublicId().toString()))
                                                .orElse(Relevance.CORE
                                                        .getPublicId()
                                                        .toString()),
                                        contextId,
                                        parentConnection.getRank(),
                                        parentConnection.getPublicId().toString(),
                                        parents);
                            })
                            .forEach(returnedContexts::add);
                }));

        return returnedContexts;
    }

    /*
     * Method recursively re-creates all Contexts entries for the entity by removing old entities and creating new ones
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void updateContexts(Node entity) {
        Set.copyOf(entity.getChildConnections())
                .forEach(childEntity -> childEntity.getChild().ifPresent(this::updateContexts));

        clearContexts(entity);

        entity.setContexts(createContexts(entity));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void clearContexts(Node entity) {
        entity.setContexts(new HashSet<>());
    }
}
