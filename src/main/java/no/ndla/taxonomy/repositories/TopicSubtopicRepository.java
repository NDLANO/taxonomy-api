package no.ndla.taxonomy.repositories;


import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.domain.TopicSubtopic;
import org.springframework.data.jpa.repository.Query;

import java.net.URI;
import java.util.List;

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
}