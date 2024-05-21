/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import java.util.*;
import no.ndla.taxonomy.config.Constants;
import no.ndla.taxonomy.domain.LanguageField;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.TaxonomyContext;
import no.ndla.taxonomy.util.HashUtil;
import no.ndla.taxonomy.util.PrettyUrlUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContextUpdaterServiceImpl implements ContextUpdaterService {

    @Value(value = "${new.url.separator:false}")
    private boolean newUrlSeparator;

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
                    "urn:relevance:core",
                    contextId,
                    0,
                    PrettyUrlUtil.createPrettyUrl(
                            Optional.empty(), node.getName(), contextId, node.getNodeType(), newUrlSeparator),
                    ""));
        }

        // Get all parent connections, append this entity publicId to the end of the actual path and add
        // all to the list to return
        node.getParentConnections()
                .forEach(parentConnection -> parentConnection.getParent().ifPresent(parent -> {
                    createContexts(parent).stream()
                            .map(parentContext -> {
                                var breadcrumbs = LanguageField.listFromLists(
                                        parentContext.breadcrumbs(), LanguageField.fromNode(parent));
                                List<String> parentIds = parentContext.parentIds();
                                parentIds.add(parent.getPublicId().toString());
                                List<String> parentContextIds = parentContext.parentContextIds();
                                parentContextIds.add(parentContext.contextId());
                                var contextId =
                                        HashUtil.mediumHash(parentContext.contextId() + parentConnection.getPublicId());
                                return new TaxonomyContext(
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
                                                .orElse("urn:relevance:core"),
                                        contextId,
                                        parentConnection.getRank(),
                                        PrettyUrlUtil.createPrettyUrl(
                                                Optional.of(parentContext
                                                        .rootName()
                                                        .fromLanguage(Constants.DefaultLanguage)),
                                                node.getName(),
                                                contextId,
                                                node.getNodeType(),
                                                newUrlSeparator),
                                        parentConnection.getPublicId().toString());
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
