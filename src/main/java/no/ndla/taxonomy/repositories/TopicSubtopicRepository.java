package no.ndla.taxonomy.repositories;


import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.domain.TopicSubtopic;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TopicSubtopicRepository extends TaxonomyRepository<TopicSubtopic> {
    List<TopicSubtopic> findByTopic(Topic topic);

    @Query("SELECT ts" +
            "   FROM TopicSubtopic ts" +
            "   JOIN FETCH ts.topic" +
            "   JOIN FETCH ts.subtopic")
    List<TopicSubtopic> findAllIncludingTopicAndSubtopic();
}