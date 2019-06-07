package no.ndla.taxonomy.repositories;


import no.ndla.taxonomy.domain.ResourceResourceType;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ResourceResourceTypeRepository extends TaxonomyRepository<ResourceResourceType> {
    @Query("SELECT rrt" +
            "   FROM ResourceResourceType rrt" +
            "   JOIN FETCH rrt.resource" +
            "   JOIN FETCH rrt.resourceType")
    List<ResourceResourceType> findAllIncludingResourceAndResourceType();
}
