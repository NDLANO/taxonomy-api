/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.repositories;

import no.ndla.taxonomy.domain.NodeConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface NodeConnectionRepository extends TaxonomyRepository<NodeConnection> {
    @SuppressWarnings("SpringDataRepositoryMethodReturnTypeInspection")
    @Query("SELECT nc.parent.id AS parentId, nc.child.id AS childId, nc.rank AS rank FROM NodeConnection nc"
            + " JOIN nc.parent JOIN nc.child WHERE nc.parent.id IN :nodeId")
    List<NodeTreeElement> findAllByNodeIdInIncludingTopicAndSubtopic(Set<Integer> nodeId);

    interface NodeTreeElement {
        Integer getParentId();

        Integer getChildId();

        Integer getRank();
    }

    @Query("SELECT nc FROM NodeConnection nc JOIN FETCH nc.parent JOIN FETCH nc.child JOIN FETCH nc.metadata m"
            + " LEFT JOIN m.grepCodes LEFT JOIN FETCH m.customFieldValues cvf LEFT JOIN cvf.customField")
    List<NodeConnection> findAllIncludingParentAndChild();

    @Query(value = "SELECT nc.id FROM NodeConnection nc", countQuery = "SELECT count(*) from NodeConnection")
    Page<Integer> findIdsPaginated(Pageable pageable);

    @Query("SELECT nc FROM NodeConnection nc JOIN FETCH nc.parent JOIN FETCH nc.child JOIN FETCH nc.metadata m"
            + " LEFT JOIN m.grepCodes LEFT JOIN FETCH m.customFieldValues cvf LEFT JOIN cvf.customField"
            + " WHERE nc.id in :ids")
    List<NodeConnection> findByIds(Collection<Integer> ids);

    @Query("SELECT DISTINCT nc FROM NodeConnection nc JOIN FETCH nc.child child JOIN FETCH nc.parent parent"
            + " JOIN FETCH nc.metadata m LEFT JOIN m.grepCodes LEFT JOIN FETCH m.customFieldValues cvf"
            + " LEFT JOIN cvf.customField LEFT JOIN FETCH child.translations WHERE parent.publicId = :publicId")
    List<NodeConnection> findAllByParentPublicIdIncludingChildAndChildTranslations(URI publicId);

    @Query("SELECT DISTINCT nc FROM NodeConnection nc JOIN FETCH nc.parent p JOIN FETCH nc.child c"
            + " LEFT JOIN p.translations LEFT JOIN FETCH c.translations LEFT JOIN c.cachedPaths"
            + " JOIN FETCH nc.metadata m LEFT JOIN m.grepCodes LEFT JOIN FETCH m.customFieldValues cvf"
            + " LEFT JOIN cvf.customField WHERE nc.child.id IN :nodeId")
    List<NodeConnection> doFindAllByChildIdIncludeTranslationsAndCachedUrlsAndFilters(Collection<Integer> nodeId);

    default List<NodeConnection> findAllByChildIdIncludeTranslationsAndCachedUrlsAndFilters(
            Collection<Integer> nodeId) {
        if (nodeId.size() == 0) {
            return List.of();
        }

        return doFindAllByChildIdIncludeTranslationsAndCachedUrlsAndFilters(nodeId);
    }

    Optional<NodeConnection> findFirstByPublicId(URI publicId);
}