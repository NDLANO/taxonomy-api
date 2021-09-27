package no.ndla.taxonomy.repositories;


import no.ndla.taxonomy.domain.NodeConnection;
import no.ndla.taxonomy.domain.TopicSubtopic;
import org.springframework.data.jpa.repository.Query;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface NodeConnectionRepository extends TaxonomyRepository<NodeConnection> {
    @SuppressWarnings("SpringDataRepositoryMethodReturnTypeInspection")
    @Query("SELECT nc.parent.id AS parentId, nc.child.id AS childId, nc.rank AS rank" +
            "   FROM NodeConnection nc" +
            "   JOIN nc.parent" +
            "   JOIN nc.child" +
            "   WHERE nc.parent.id IN :nodeId")
    List<NodeTreeElement> findAllByNodeIdInIncludingTopicAndSubtopic(Set<Integer> nodeId);

    interface NodeTreeElement {
        Integer getParentId();

        Integer getChildId();

        Integer getRank();
    }

    @Query("SELECT nc" +
            "   FROM NodeConnection nc" +
            "   JOIN FETCH nc.parent" +
            "   JOIN FETCH nc.child")
    List<NodeConnection> findAllIncludingParentAndChild();

    @Query("SELECT DISTINCT nc" +
            "   FROM NodeConnection nc" +
            "   JOIN FETCH nc.parent p" +
            "   JOIN FETCH nc.child c" +
            "   LEFT JOIN FETCH p.cachedPaths" +
            "   WHERE c.publicId = :childPublicId")
    List<NodeConnection> findAllByChildPublicIdIncludingParentAndChildAndCachedUrls(URI childPublicId);

    @Query("SELECT DISTINCT nc" +
            "   FROM NodeConnection nc" +
            "   JOIN FETCH nc.child child" +
            "   JOIN nc.parent parent" +
            "   LEFT JOIN FETCH child.translations" +
            "   WHERE parent.publicId = :publicId")
    List<NodeConnection> findAllByParentPublicIdIncludingChildAndChildTranslations(URI publicId);

    @Query("SELECT DISTINCT nc" +
            "   FROM NodeConnection nc" +
            "   LEFT JOIN FETCH nc.parent p" +
            "   LEFT JOIN FETCH nc.child c" +
            "   LEFT JOIN FETCH p.translations" +
            "   LEFT JOIN FETCH c.translations" +
            "   LEFT JOIN FETCH c.cachedPaths" +
            "  WHERE nc.child.id IN :nodeId")
    List<NodeConnection> doFindAllByChildIdIncludeTranslationsAndCachedUrlsAndFilters(Collection<Integer> nodeId);

    default List<NodeConnection> findAllByChildIdIncludeTranslationsAndCachedUrlsAndFilters(Collection<Integer> nodeId) {
        if (nodeId.size() == 0) {
            return List.of();
        }

        return doFindAllByChildIdIncludeTranslationsAndCachedUrlsAndFilters(nodeId);
    }

    Optional<TopicSubtopic> findFirstByPublicId(URI publicId);
}