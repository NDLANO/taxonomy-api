package no.ndla.taxonomy.repositories;


import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.domain.TopicSubtopic;
import org.springframework.data.jpa.repository.Query;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TopicSubtopicRepository extends TaxonomyRepository<TopicSubtopic> {
    List<TopicSubtopic> findByTopic(Topic topic);

    @Query("SELECT ts" +
            "   FROM TopicSubtopic ts" +
            "   JOIN FETCH ts.topic" +
            "   JOIN FETCH ts.subtopic")
    List<TopicSubtopic> findAllIncludingTopicAndSubtopic();

    @Query("SELECT DISTINCT ts" +
            "   FROM TopicSubtopic ts" +
            "   JOIN FETCH ts.topic t" +
            "   JOIN FETCH ts.subtopic st" +
            "   LEFT JOIN FETCH t.cachedPaths" +
            "   WHERE st.publicId = :subTopicPublicId")
    List<TopicSubtopic> findAllBySubtopicPublicIdIncludingTopicAndSubtopicAndCachedUrls(URI subTopicPublicId);

    @Query("SELECT DISTINCT ts" +
            "   FROM TopicSubtopic ts" +
            "   JOIN ts.topic parentTopic" +
            "   JOIN FETCH ts.subtopic subTopicTopic" +
            "   JOIN subTopicTopic.topicFilters subTopic_filter" +
            "   JOIN subTopic_filter.filter subTopicFilter_filter" +
            "   LEFT JOIN FETCH subTopicTopic.translations" +
            "   WHERE" +
            "       parentTopic.publicId = :publicId AND " +
            "       subTopicFilter_filter.publicId IN :filterPublicIds")
    List<TopicSubtopic> doFindAllByTopicPublicIdAndFilterPublicIdsIncludingSubtopicAndSubtopicTranslations(URI publicId, Collection<URI> filterPublicIds);

    default List<TopicSubtopic> findAllByTopicPublicIdAndFilterPublicIdsIncludingSubtopicAndSubtopicTranslations(URI publicId, Collection<URI> filterPublicIds) {
        if (filterPublicIds.size() == 0) {
            return doFindAllByTopicPublicIdAndFilterPublicIdsIncludingSubtopicAndSubtopicTranslations(publicId, null);
        }

        return doFindAllByTopicPublicIdAndFilterPublicIdsIncludingSubtopicAndSubtopicTranslations(publicId, filterPublicIds);
    }

    @Query("SELECT DISTINCT ts" +
            "   FROM TopicSubtopic ts" +
            "   JOIN FETCH ts.subtopic subTopicTopic" +
            "   JOIN ts.topic parentTopic" +
            "   LEFT JOIN FETCH subTopicTopic.translations" +
            "   WHERE" +
            "       parentTopic.publicId = :publicId")
    List<TopicSubtopic> findAllByTopicPublicIdIncludingSubtopicAndSubtopicTranslations(URI publicId);

    @Query("SELECT DISTINCT ts" +
            "   FROM TopicSubtopic ts" +
            "   LEFT JOIN FETCH ts.topic t" +
            "   LEFT JOIN FETCH ts.subtopic st" +
            "   LEFT JOIN FETCH t.translations" +
            "   LEFT JOIN FETCH st.translations" +
            "   LEFT JOIN FETCH st.cachedPaths" +
            "   LEFT JOIN FETCH st.topicFilters tf" +
            "   LEFT JOIN FETCH tf.filter f" +
            "   LEFT JOIN FETCH f.translations" +
            "  WHERE ts.subtopic.id IN :subTopicId")
    List<TopicSubtopic> doFindAllBySubtopicIdIncludeTranslationsAndCachedUrlsAndFilters(Collection<Integer> subTopicId);

    default List<TopicSubtopic> findAllBySubtopicIdIncludeTranslationsAndCachedUrlsAndFilters(Collection<Integer> subTopicId) {
        if (subTopicId.size() == 0) {
            return List.of();
        }

        return doFindAllBySubtopicIdIncludeTranslationsAndCachedUrlsAndFilters(subTopicId);
    }

    Optional<TopicSubtopic> findFirstByPublicId(URI publicId);
}