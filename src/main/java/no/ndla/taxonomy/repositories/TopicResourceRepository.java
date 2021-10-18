/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.repositories;

import no.ndla.taxonomy.domain.TopicResource;
import org.springframework.data.jpa.repository.Query;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface TopicResourceRepository extends TaxonomyRepository<TopicResource> {
    @Query(
            "SELECT tr"
                    + "   FROM TopicResource tr"
                    + "   JOIN FETCH tr.topic"
                    + "   JOIN FETCH tr.resource")
    List<TopicResource> findAllIncludingTopicAndResource();

    @Query(
            "SELECT DISTINCT tr"
                    + "   FROM TopicResource tr"
                    + "   LEFT JOIN FETCH tr.topic t"
                    + "   LEFT JOIN FETCH tr.resource r"
                    + "   LEFT JOIN FETCH tr.relevance rel"
                    + "   LEFT JOIN FETCH r.resourceTranslations"
                    + "   LEFT JOIN FETCH r.cachedPaths"
                    + "   LEFT JOIN FETCH r.resourceResourceTypes rrtFetch"
                    + "   LEFT JOIN FETCH rrtFetch.resourceType rtFetch"
                    + "   LEFT JOIN FETCH rtFetch.resourceTypeTranslations"
                    + "   WHERE "
                    + "       t.id IN :topicIds")
    List<TopicResource> findAllByTopicIdsIncludingRelationsForResourceDocuments(
            Collection<Integer> topicIds);

    @Query(
            "SELECT DISTINCT tr"
                    + "   FROM TopicResource tr"
                    + "   LEFT JOIN FETCH tr.topic t"
                    + "   LEFT JOIN FETCH tr.resource r"
                    + "   LEFT JOIN tr.relevance rel"
                    + "   LEFT JOIN FETCH r.resourceTranslations"
                    + "   LEFT JOIN FETCH r.cachedPaths"
                    + "   LEFT JOIN FETCH r.resourceResourceTypes rrtFetch"
                    + "   LEFT JOIN FETCH tr.relevance"
                    + "   LEFT JOIN FETCH rrtFetch.resourceType rtFetch"
                    + "   LEFT JOIN FETCH rtFetch.resourceTypeTranslations"
                    + "   WHERE "
                    + "       t.id IN :topicIds AND"
                    + "       (:relevancePublicId IS NULL OR rel.publicId = :relevancePublicId)")
    List<TopicResource>
            doFindAllByTopicIdsAndRelevancePublicIdIfNotNullIncludingRelationsForResourceDocuments(
                    Collection<Integer> topicIds, URI relevancePublicId);

    default List<TopicResource>
            findAllByTopicIdsAndRelevancePublicIdIfNotNullIncludingRelationsForResourceDocuments(
                    Collection<Integer> topicIds, URI relevancePublicId) {
        if (topicIds.size() == 0) {
            return doFindAllByTopicIdsAndRelevancePublicIdIfNotNullIncludingRelationsForResourceDocuments(
                    null, relevancePublicId);
        }

        return doFindAllByTopicIdsAndRelevancePublicIdIfNotNullIncludingRelationsForResourceDocuments(
                topicIds, relevancePublicId);
    }

    @Query(
            "SELECT DISTINCT tr"
                    + "   FROM TopicResource tr"
                    + "   JOIN FETCH tr.resource r"
                    + "   LEFT JOIN FETCH tr.topic t"
                    + "   LEFT JOIN tr.relevance rel"
                    + "   LEFT JOIN FETCH r.resourceTranslations"
                    + "   LEFT JOIN FETCH r.cachedPaths"
                    + "   LEFT JOIN FETCH r.resourceResourceTypes rrtFetch"
                    + "   LEFT JOIN FETCH rrtFetch.resourceType rtFetch"
                    + "   LEFT JOIN FETCH rtFetch.resourceTypeTranslations"
                    + "   WHERE t.id IN :topicIds AND "
                    + "       (:relevancePublicId IS NULL OR rel.publicId = :relevancePublicId)")
    List<TopicResource>
            doFindAllByTopicIdsAndResourceFilterFilterPublicIdsAndRelevancePublicIdIfNotNullIncludingRelationsForResourceDocuments(
                    Collection<Integer> topicIds, URI relevancePublicId);

    default List<TopicResource>
            findAllByTopicIdsAndResourceFilterFilterPublicIdsAndRelevancePublicIdIfNotNullIncludingRelationsForResourceDocuments(
                    Collection<Integer> topicIds, URI relevancePublicId) {
        if (topicIds.size() == 0) {
            topicIds = null;
        }

        return doFindAllByTopicIdsAndResourceFilterFilterPublicIdsAndRelevancePublicIdIfNotNullIncludingRelationsForResourceDocuments(
                topicIds, relevancePublicId);
    }

    @Query(
            "SELECT DISTINCT tr"
                    + "   FROM TopicResource tr"
                    + "   INNER JOIN FETCH tr.resource r"
                    + "   LEFT JOIN FETCH tr.topic t"
                    + "   LEFT JOIN tr.relevance rel"
                    + "   LEFT JOIN r.resourceResourceTypes rrt "
                    + "   LEFT JOIN rrt.resourceType rt"
                    + "   LEFT JOIN FETCH r.resourceTranslations"
                    + "   LEFT JOIN FETCH r.cachedPaths"
                    + "   LEFT JOIN FETCH r.resourceResourceTypes rrtFetch"
                    + "   LEFT JOIN FETCH tr.relevance"
                    + "   LEFT JOIN FETCH rrtFetch.resourceType rtFetch"
                    + "   LEFT JOIN FETCH rtFetch.resourceTypeTranslations"
                    + "   WHERE t.id IN :topicIds AND"
                    + "       (rt.publicId IN :resourceTypePublicIds) AND"
                    + "       (:relevancePublicId IS NULL OR rel.publicId = :relevancePublicId)")
    List<TopicResource>
            doFindAllByTopicIdsAndResourceFilterFilterPublicIdsAndResourceTypePublicIdsAndRelevancePublicIdIfNotNullIncludingRelationsForResourceDocuments(
                    Collection<Integer> topicIds,
                    Set<URI> resourceTypePublicIds,
                    URI relevancePublicId);

    default List<TopicResource>
            findAllByTopicIdsAndResourceFilterFilterPublicIdsAndResourceTypePublicIdsAndRelevancePublicIdIfNotNullIncludingRelationsForResourceDocuments(
                    Collection<Integer> topicIds,
                    Set<URI> resourceTypePublicIds,
                    URI relevancePublicId) {
        if (topicIds.size() == 0) {
            topicIds = null;
        }

        if (resourceTypePublicIds.size() == 0) {
            resourceTypePublicIds = null;
        }

        return doFindAllByTopicIdsAndResourceFilterFilterPublicIdsAndResourceTypePublicIdsAndRelevancePublicIdIfNotNullIncludingRelationsForResourceDocuments(
                topicIds, resourceTypePublicIds, relevancePublicId);
    }

    @Query(
            "SELECT DISTINCT tr"
                    + "   FROM TopicResource tr"
                    + "   JOIN FETCH tr.resource r"
                    + "   LEFT JOIN FETCH tr.topic t"
                    + "   LEFT JOIN tr.relevance rel"
                    + "   LEFT JOIN r.resourceResourceTypes rrt "
                    + "   LEFT JOIN rrt.resourceType rt"
                    + "   LEFT JOIN FETCH r.resourceTranslations"
                    + "   LEFT JOIN FETCH r.cachedPaths"
                    + "   LEFT JOIN FETCH r.resourceResourceTypes rrtFetch"
                    + "   LEFT JOIN FETCH tr.relevance"
                    + "   LEFT JOIN FETCH rrtFetch.resourceType rtFetch"
                    + "   LEFT JOIN FETCH rtFetch.resourceTypeTranslations"
                    + "   WHERE t.id IN :topicIds AND"
                    + "       (rt.publicId IN :resourceTypePublicIds) AND"
                    + "       (:relevancePublicId IS NULL OR rel.publicId = :relevancePublicId)")
    List<TopicResource>
            doFindAllByTopicIdsAndResourceTypePublicIdsAndRelevancePublicIdIfNotNullIncludingRelationsForResourceDocuments(
                    Collection<Integer> topicIds,
                    Collection<URI> resourceTypePublicIds,
                    URI relevancePublicId);

    default List<TopicResource>
            findAllByTopicIdsAndResourceTypePublicIdsAndRelevancePublicIdIfNotNullIncludingRelationsForResourceDocuments(
                    Collection<Integer> topicIds,
                    Collection<URI> resourceTypePublicIds,
                    URI relevancePublicId) {
        if (topicIds.size() == 0) {
            topicIds = null;
        }

        if (resourceTypePublicIds.size() == 0) {
            resourceTypePublicIds = null;
        }

        return doFindAllByTopicIdsAndResourceTypePublicIdsAndRelevancePublicIdIfNotNullIncludingRelationsForResourceDocuments(
                topicIds, resourceTypePublicIds, relevancePublicId);
    }

    Optional<TopicResource> findFirstByPublicId(URI publicId);
}
