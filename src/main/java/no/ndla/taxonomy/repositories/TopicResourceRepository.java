package no.ndla.taxonomy.repositories;


import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.domain.TopicResource;
import org.springframework.data.jpa.repository.Query;

import java.net.URI;
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
}