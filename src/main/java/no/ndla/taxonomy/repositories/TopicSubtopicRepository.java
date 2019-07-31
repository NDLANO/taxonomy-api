package no.ndla.taxonomy.repositories;


import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.domain.TopicSubtopic;
import org.springframework.data.jpa.repository.Query;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface TopicSubtopicRepository extends TaxonomyRepository<TopicSubtopic> {
    List<TopicSubtopic> findByTopic(Topic topic);

    @Query("SELECT ts" +
            "   FROM TopicSubtopic ts" +
            "   JOIN FETCH ts.topic" +
            "   JOIN FETCH ts.subtopic")
    List<TopicSubtopic> findAllIncludingTopicAndSubtopic();

    @Query("SELECT DISTINCT ts" +
            "   FROM TopicSubtopic ts" +
            "   INNER JOIN FETCH ts.topic t" +
            "   INNER JOIN FETCH ts.subtopic st" +
            "   LEFT OUTER JOIN FETCH t.cachedUrls" +
            "   WHERE st.publicId = :subTopicPublicId")
    List<TopicSubtopic> findAllBySubtopicPublicIdIncludingTopicAndSubtopicAndCachedUrls(URI subTopicPublicId);

    @Query("SELECT DISTINCT ts" +
            "   FROM TopicSubtopic ts" +
            "   INNER JOIN FETCH ts.topic t" +
            "   INNER JOIN FETCH ts.subtopic st" +
            "   LEFT OUTER JOIN FETCH st.cachedUrls" +
            "   WHERE t.publicId = :topicPublicId")
    List<TopicSubtopic> findAllByTopicPublicIdIncludingTopicAndSubtopicAndCachedUrls(URI topicPublicId);

    @Query("SELECT DISTINCT ts" +
            "   FROM TopicSubtopic ts" +
            "   INNER JOIN ts.topic parentTopic" +
            "   INNER JOIN FETCH ts.subtopic subTopicTopic" +
            "   INNER JOIN subTopicTopic.topicFilters subTopic_filter" +
            "   INNER JOIN subTopic_filter.filter subTopicFilter_filter" +
            "   LEFT OUTER JOIN FETCH subTopicTopic.translations" +
            "   WHERE" +
            "       parentTopic.publicId = :publicId AND " +
            "       subTopicFilter_filter.publicId IN :filterPublicIds")
    List<TopicSubtopic> findAllByTopicPublicIdAndFilterPublicIdsIncludingSubtopicAndSubtopicTranslations(URI publicId, Set<URI> filterPublicIds);

    @Query("SELECT DISTINCT ts" +
            "   FROM TopicSubtopic ts" +
            "   INNER JOIN FETCH ts.subtopic subTopicTopic" +
            "   INNER JOIN ts.topic parentTopic" +
            "   LEFT OUTER JOIN FETCH subTopicTopic.translations" +
            "   WHERE" +
            "       parentTopic.publicId = :publicId")
    List<TopicSubtopic> findAllByTopicPublicIdIncludingSubtopicAndSubtopicTranslations(URI publicId);

    @Query("SELECT DISTINCT ts" +
            "   FROM TopicSubtopic ts" +
            "   LEFT JOIN FETCH ts.topic t" +
            "   LEFT JOIN FETCH ts.subtopic st" +
            "   LEFT OUTER JOIN FETCH t.translations" +
            "   LEFT OUTER JOIN FETCH st.translations" +
            "   LEFT OUTER JOIN FETCH st.cachedUrls" +
            "   LEFT OUTER JOIN FETCH st.topicFilters tf" +
            "   LEFT OUTER JOIN FETCH tf.filter f" +
            "   LEFT OUTER JOIN FETCH f.translations" +
            "  WHERE ts.subtopic.id IN :subTopicId")
    List<TopicSubtopic> findAllBySubtopicIdIncludeTranslationsAndCachedUrlsAndFilters(Collection<Integer> subTopicId);
}