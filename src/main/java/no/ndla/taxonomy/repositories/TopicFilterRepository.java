package no.ndla.taxonomy.repositories;


import no.ndla.taxonomy.domain.TopicFilter;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TopicFilterRepository extends TaxonomyRepository<TopicFilter> {
    @Query("SELECT tf" +
            "   FROM TopicFilter tf" +
            "   JOIN FETCH tf.topic" +
            "   JOIN FETCH tf.filter" +
            "   JOIN FETCH tf.relevance")
    List<TopicFilter> findAllIncludingTopicAndFilterAndRelevance();
}
