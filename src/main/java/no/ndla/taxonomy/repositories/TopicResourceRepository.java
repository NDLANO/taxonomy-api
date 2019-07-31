package no.ndla.taxonomy.repositories;


import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.domain.TopicResource;
import org.springframework.data.jpa.repository.Query;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface TopicResourceRepository extends TaxonomyRepository<TopicResource> {
    List<TopicResource> findByTopic(Topic topic);

    @Query("SELECT tr" +
            "   FROM TopicResource tr" +
            "   JOIN FETCH tr.topic" +
            "   JOIN FETCH tr.resource")
    List<TopicResource> findAllIncludingTopicAndResource();

    @Query("SELECT DISTINCT tr" +
            "   FROM TopicResource tr" +
            "   LEFT JOIN FETCH tr.topic t" +
            "   LEFT JOIN FETCH tr.resource r" +
            "   LEFT JOIN FETCH r.filters rf" +
            "   LEFT JOIN FETCH rf.filter" +
            "   LEFT JOIN rf.relevance rel" +
            "   LEFT JOIN FETCH r.resourceTranslations" +
            "   LEFT JOIN FETCH r.cachedUrls" +
            "   LEFT JOIN FETCH r.resourceResourceTypes rrtFetch" +
            "   LEFT JOIN FETCH rf.relevance" +
            "   LEFT JOIN FETCH rrtFetch.resourceType rtFetch" +
            "   LEFT JOIN FETCH rtFetch.resourceTypeTranslations" +
            "   WHERE " +
            "       t.id IN :topicIds AND" +
            "       (:relevancePublicId IS NULL OR rel.publicId = :relevancePublicId)")
    List<TopicResource> findAllByTopicIdsAndRelevancePublicIdIfNotNullIncludingRelationsForResourceDocuments(Collection<Integer> topicIds, URI relevancePublicId);

    @Query("SELECT DISTINCT tr" +
            "   FROM TopicResource tr" +
            "   JOIN FETCH tr.resource r" +
            "   LEFT JOIN FETCH r.filters rf" +
            "   LEFT JOIN FETCH rf.filter" +
            "   JOIN rf.filter f" +
            "   LEFT JOIN FETCH tr.topic t" +
            "   LEFT JOIN rf.relevance rel" +
            "   LEFT JOIN FETCH r.resourceTranslations" +
            "   LEFT JOIN FETCH r.cachedUrls" +
            "   LEFT JOIN FETCH r.resourceResourceTypes rrtFetch" +
            "   LEFT JOIN FETCH rf.relevance" +
            "   LEFT JOIN FETCH rrtFetch.resourceType rtFetch" +
            "   LEFT JOIN FETCH rtFetch.resourceTypeTranslations" +
            "   WHERE t.id IN :topicIds AND f.publicId IN :filterPublicIds AND " +
            "       (:relevancePublicId IS NULL OR rel.publicId = :relevancePublicId)")
    List<TopicResource> findAllByTopicIdsAndResourceFilterFilterPublicIdsAndRelevancePublicIdIfNotNullIncludingRelationsForResourceDocuments(Collection<Integer> topicIds, Set<URI> filterPublicIds, URI relevancePublicId);

    @Query("SELECT DISTINCT tr" +
            "   FROM TopicResource tr" +
            "   INNER JOIN FETCH tr.resource r" +
            "   LEFT JOIN FETCH r.filters rf" +
            "   LEFT JOIN FETCH rf.filter" +
            "   LEFT JOIN rf.filter f" +
            "   LEFT JOIN FETCH tr.topic t" +
            "   LEFT JOIN rf.relevance rel" +
            "   LEFT JOIN r.resourceResourceTypes rrt " +
            "   LEFT JOIN rrt.resourceType rt" +
            "   LEFT JOIN FETCH r.resourceTranslations" +
            "   LEFT JOIN FETCH r.cachedUrls" +
            "   LEFT JOIN FETCH r.resourceResourceTypes rrtFetch" +
            "   LEFT JOIN FETCH rf.relevance" +
            "   LEFT JOIN FETCH rrtFetch.resourceType rtFetch" +
            "   LEFT JOIN FETCH rtFetch.resourceTypeTranslations" +
            "   WHERE t.id IN :topicIds AND f.publicId IN :filterPublicIds AND" +
            "       (rt.publicId IN :resourceTypePublicIds) AND" +
            "       (:relevancePublicId IS NULL OR rel.publicId = :relevancePublicId)")
    List<TopicResource> findAllByTopicIdsAndResourceFilterFilterPublicIdsAndResourceTypePublicIdsAndRelevancePublicIdIfNotNullIncludingRelationsForResourceDocuments(Collection<Integer> topicIds, Set<URI> filterPublicIds, Set<URI> resourceTypePublicIds, URI relevancePublicId);

    @Query("SELECT DISTINCT tr" +
            "   FROM TopicResource tr" +
            "   JOIN FETCH tr.resource r" +
            "   LEFT JOIN FETCH r.filters rf" +
            "   LEFT JOIN FETCH rf.filter" +
            "   LEFT JOIN FETCH tr.topic t" +
            "   LEFT JOIN rf.relevance rel" +
            "   LEFT JOIN r.resourceResourceTypes rrt " +
            "   LEFT JOIN rrt.resourceType rt" +
            "   LEFT JOIN FETCH r.resourceTranslations" +
            "   LEFT JOIN FETCH r.cachedUrls" +
            "   LEFT JOIN FETCH r.resourceResourceTypes rrtFetch" +
            "   LEFT JOIN FETCH rf.relevance" +
            "   LEFT JOIN FETCH rrtFetch.resourceType rtFetch" +
            "   LEFT JOIN FETCH rtFetch.resourceTypeTranslations" +
            "   WHERE t.id IN :topicIds AND" +
            "       (rt.publicId IN :resourceTypePublicIds) AND" +
            "       (:relevancePublicId IS NULL OR rel.publicId = :relevancePublicId)")
    List<TopicResource> findAllByTopicIdsAndResourceTypePublicIdsAndRelevancePublicIdIfNotNullIncludingRelationsForResourceDocuments(Collection<Integer> topicIds, Set<URI> resourceTypePublicIds, URI relevancePublicId);
}