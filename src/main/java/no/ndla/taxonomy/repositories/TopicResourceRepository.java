package no.ndla.taxonomy.repositories;


import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.domain.TopicResource;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TopicResourceRepository extends TaxonomyRepository<TopicResource> {
    List<TopicResource> findByTopic(Topic topic);

    @Query("SELECT tr" +
            "   FROM TopicResource tr" +
            "   JOIN FETCH tr.topic" +
            "   JOIN FETCH tr.resource")
    List<TopicResource> findAllIncludingTopicAndResource();
}