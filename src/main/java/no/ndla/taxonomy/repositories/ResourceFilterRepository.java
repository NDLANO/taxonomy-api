package no.ndla.taxonomy.repositories;


import no.ndla.taxonomy.domain.ResourceFilter;
import org.springframework.data.jpa.repository.Query;

import java.net.URI;
import java.util.List;

public interface ResourceFilterRepository extends TaxonomyRepository<ResourceFilter> {
    @Query("SELECT rf" +
            "   FROM ResourceFilter rf" +
            "   JOIN FETCH rf.filter" +
            "   JOIN FETCH rf.relevance" +
            "   JOIN FETCH rf.resource")
    List<ResourceFilter> findAllIncludingResourceAndFilterAndRelevance();

    @Query("SELECT rf" +
            "   FROM ResourceFilter rf" +
            "   JOIN FETCH rf.filter f" +
            "   JOIN FETCH rf.resource r" +
            "   LEFT JOIN FETCH rf.relevance" +
            "   WHERE r.publicId = :resourcePublicId")
    List<ResourceFilter> findAllByResourcePublicIdIncludingResourceAndFilterAndRelevance(URI resourcePublicId);
}
