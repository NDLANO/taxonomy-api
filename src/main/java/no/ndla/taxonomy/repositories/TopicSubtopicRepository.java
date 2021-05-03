package no.ndla.taxonomy.repositories;


import no.ndla.taxonomy.domain.TopicSubtopic;
import org.springframework.data.jpa.repository.Query;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface TopicSubtopicRepository extends TaxonomyRepository<TopicSubtopic> {
    @SuppressWarnings("SpringDataRepositoryMethodReturnTypeInspection")
    @Query("SELECT ts.topic.id AS topicId, ts.subtopic.id AS subtopicId, ts.rank AS rank" +
            "   FROM TopicSubtopic ts" +
            "   JOIN ts.topic" +
            "   JOIN ts.subtopic" +
            "   WHERE ts.topic.id IN :topicId")
    List<TopicTreeElement> findAllByTopicIdInIncludingTopicAndSubtopic(Set<Integer> topicId);

    interface TopicTreeElement {
        Integer getTopicId();

        Integer getSubtopicId();

        Integer getRank();
    }

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