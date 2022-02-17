/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.repositories;

import no.ndla.taxonomy.domain.ResourceResourceType;
import org.springframework.data.jpa.repository.Query;

import java.net.URI;
import java.util.List;

public interface ResourceResourceTypeRepository extends TaxonomyRepository<ResourceResourceType> {
    @Query("SELECT rrt FROM ResourceResourceType rrt JOIN FETCH rrt.resource JOIN FETCH rrt.resourceType")
    List<ResourceResourceType> findAllIncludingResourceAndResourceType();

    @Query("SELECT rrt FROM ResourceResourceType rrt JOIN FETCH rrt.resource"
            + " JOIN FETCH rrt.resourceType rt LEFT JOIN FETCH rt.parent"
            + " WHERE rrt.resource.publicId = :resourcePublicId")
    List<ResourceResourceType> findAllByResourcePublicIdIncludingResourceAndResourceTypeAndResourceTypeParent(
            URI resourcePublicId);
}
