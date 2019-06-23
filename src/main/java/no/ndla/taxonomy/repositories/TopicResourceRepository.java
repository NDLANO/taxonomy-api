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
            "   INNER JOIN tr.topic tFilter" +
            "   INNER JOIN FETCH tr.resource r" +
            "   LEFT OUTER JOIN r.resourceResourceTypes rrtFilter" +
            "   LEFT OUTER JOIN rrtFilter.resourceType rtFilter" +
            "   LEFT OUTER JOIN r.filters rfFilter" +
            "   LEFT OUTER JOIN rfFilter.filter fFilter" +
            "   LEFT OUTER JOIN FETCH r.resourceTranslations" +
            "   LEFT OUTER JOIN FETCH r.resourceResourceTypes rrt" +
            "   LEFT OUTER JOIN FETCH rrt.resourceType" +
            "   LEFT OUTER JOIN FETCH r.cachedUrls" +
            "   WHERE tFilter = :topic AND" +
            "       rtFilter.publicId IN :resourceTypePublicIds AND" +
            "       fFilter.publicId IN :filterPublicIds" +
            "   ORDER BY tr.rank")
    List<TopicResource> findAllByTopicAndFilterPublicIdsAndResourceTypePublicIdsIncludingResourceAndResourceTranslationsAndResourceTypesAndResourceTypeTranslationsAndCachedUrls(Topic topic, Set<URI> filterPublicIds, Set<URI> resourceTypePublicIds);

    @Query("SELECT DISTINCT tr" +
            "   FROM TopicResource tr" +
            "   INNER JOIN tr.topic tFilter" +
            "   INNER JOIN FETCH tr.resource r" +
            "   LEFT OUTER JOIN r.filters rfFilter" +
            "   LEFT OUTER JOIN rfFilter.filter fFilter" +
            "   LEFT OUTER JOIN FETCH r.resourceTranslations" +
            "   LEFT OUTER JOIN FETCH r.resourceResourceTypes rrt" +
            "   LEFT OUTER JOIN FETCH rrt.resourceType" +
            "   LEFT OUTER JOIN FETCH r.cachedUrls" +
            "   WHERE tFilter = :topic AND" +
            "       fFilter.publicId IN :filterPublicIds" +
            "   ORDER BY tr.rank")
    List<TopicResource> findAllByTopicAndFilterPublicIdsIncludingResourceAndResourceTranslationsAndResourceTypesAndResourceTypeTranslationsAndCachedUrls(Topic topic, Set<URI> filterPublicIds);

    @Query("SELECT DISTINCT tr" +
            "   FROM TopicResource tr" +
            "   INNER JOIN tr.topic tFilter" +
            "   INNER JOIN FETCH tr.resource r" +
            "   LEFT OUTER JOIN r.resourceResourceTypes rrtFilter" +
            "   LEFT OUTER JOIN rrtFilter.resourceType rtFilter" +
            "   LEFT OUTER JOIN FETCH r.resourceTranslations" +
            "   LEFT OUTER JOIN FETCH r.resourceResourceTypes rrt" +
            "   LEFT OUTER JOIN FETCH rrt.resourceType" +
            "   LEFT OUTER JOIN FETCH r.cachedUrls" +
            "   WHERE tFilter = :topic AND" +
            "       rtFilter.publicId IN :resourceTypePublicIds" +
            "   ORDER BY tr.rank")
    List<TopicResource> findAllByTopicAndResourceTypePublicIdsIncludingResourceAndResourceTranslationsAndResourceTypesAndResourceTypeTranslationsAndCachedUrls(Topic topic, Set<URI> resourceTypePublicIds);

    @Query("SELECT DISTINCT tr" +
            "   FROM TopicResource tr" +
            "   INNER JOIN tr.topic tFilter" +
            "   LEFT OUTER JOIN FETCH tr.resource r" +
            "   LEFT OUTER JOIN FETCH r.resourceTranslations" +
            "   LEFT OUTER JOIN FETCH r.resourceResourceTypes rrt" +
            "   LEFT OUTER JOIN FETCH rrt.resourceType" +
            "   LEFT OUTER JOIN FETCH r.cachedUrls" +
            "   WHERE tFilter = :topic" +
            "   ORDER BY tr.rank")
    List<TopicResource> findAllByTopicIncludingResourceAndResourceTranslationsAndResourceTypesAndResourceTypeTranslationsAndCachedUrls(Topic topic);

    @Query("SELECT DISTINCT tr" +
            "   FROM TopicResource tr" +
            "   LEFT OUTER JOIN tr.topic t" +
            "   LEFT JOIN FETCH tr.resource r" +
            "   LEFT JOIN r.filters rf" +
            "   LEFT JOIN rf.relevance rel" +
            "   LEFT OUTER JOIN FETCH r.resourceTranslations" +
            "   LEFT OUTER JOIN FETCH r.cachedUrls" +
            "   LEFT OUTER JOIN FETCH r.resourceResourceTypes" +
            "   WHERE " +
            "       t.id IN :topicIds AND" +
            "       (:relevancePublicId IS NULL OR rel.publicId = :relevancePublicId)")
    List<TopicResource> findAllByTopicIdsAndRelevancePublicIdIfNotNullIncludingResourceAndResourceTranslationsAndCachedUrlsAndResourceResourceTypes(Collection<Integer> topicIds, URI relevancePublicId);

    @Query("SELECT DISTINCT tr" +
            "   FROM TopicResource tr" +
            "   INNER JOIN FETCH tr.resource r" +
            "   LEFT JOIN r.filters rf" +
            "   INNER JOIN rf.filter f" +
            "   LEFT OUTER JOIN tr.topic t" +
            "   LEFT JOIN rf.relevance rel" +
            "   LEFT OUTER JOIN FETCH r.resourceTranslations" +
            "   LEFT OUTER JOIN FETCH r.cachedUrls" +
            "   LEFT OUTER JOIN FETCH r.resourceResourceTypes" +
            "   WHERE t.id IN :topicIds AND f.publicId IN :filterPublicIds AND " +
            "       (:relevancePublicId IS NULL OR rel.publicId = :relevancePublicId)")
    List<TopicResource> findAllByTopicIdsAndResourceFilterFilterPublicIdsAndRelevancePublicIdIfNotNullIncludingResourceAndResourceTranslationsAndCachedUrlsAndResourceResourceTypes(Collection<Integer> topicIds, Set<URI> filterPublicIds, URI relevancePublicId);

    @Query("SELECT DISTINCT tr" +
            "   FROM TopicResource tr" +
            "   INNER JOIN FETCH tr.resource r" +
            "   INNER JOIN r.filters rf" +
            "   LEFT JOIN rf.filter f" +
            "   LEFT OUTER JOIN tr.topic t" +
            "   LEFT JOIN rf.relevance rel" +
            "   LEFT JOIN r.resourceResourceTypes rrt " +
            "   LEFT JOIN rrt.resourceType rt" +
            "   LEFT OUTER JOIN FETCH r.resourceTranslations" +
            "   LEFT OUTER JOIN FETCH r.cachedUrls" +
            "   LEFT OUTER JOIN FETCH r.resourceResourceTypes" +
            "   WHERE t.id IN :topicIds AND f.publicId IN :filterPublicIds AND" +
            "       (rt.publicId IN :resourceTypePublicIds) AND" +
            "       (:relevancePublicId IS NULL OR rel.publicId = :relevancePublicId)")
    List<TopicResource> findAllByTopicIdsAndResourceFilterFilterPublicIdsAndResourceTypePublicIdsAndRelevancePublicIdIfNotNullIncludingResourceAndResourceTranslationsAndCachedUrlsAndResourceResourceTypes(Collection<Integer> topicIds, Set<URI> filterPublicIds, Set<URI> resourceTypePublicIds, URI relevancePublicId);

    @Query("SELECT DISTINCT tr" +
            "   FROM TopicResource tr" +
            "   INNER JOIN FETCH tr.resource r" +
            "   LEFT JOIN r.filters rf" +
            "   LEFT OUTER JOIN tr.topic t" +
            "   LEFT JOIN rf.relevance rel" +
            "   LEFT OUTER JOIN r.resourceResourceTypes rrt " +
            "   LEFT JOIN rrt.resourceType rt" +
            "   LEFT OUTER JOIN FETCH r.resourceTranslations" +
            "   LEFT OUTER JOIN FETCH r.cachedUrls" +
            "   LEFT OUTER JOIN FETCH r.resourceResourceTypes" +
            "   WHERE t.id IN :topicIds AND" +
            "       (rt.publicId IN :resourceTypePublicIds) AND" +
            "       (:relevancePublicId IS NULL OR rel.publicId = :relevancePublicId)")
    List<TopicResource> findAllByTopicIdsAndResourceTypePublicIdsAndRelevancePublicIdIfNotNullIncludingResourceAndResourceTranslationsAndCachedUrlsAndResourceResourceTypes(Collection<Integer> topicIds, Set<URI> resourceTypePublicIds, URI relevancePublicId);
}