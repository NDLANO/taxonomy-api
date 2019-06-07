package no.ndla.taxonomy.repositories;


import no.ndla.taxonomy.domain.ResourceFilter;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ResourceFilterRepository extends TaxonomyRepository<ResourceFilter> {
    @Query("SELECT rf" +
            "   FROM ResourceFilter rf" +
            "   JOIN FETCH rf.filter" +
            "   JOIN FETCH rf.relevance" +
            "   JOIN FETCH rf.resource")
    List<ResourceFilter> findAllIncludingResourceAndFilterAndRelevance();
}
