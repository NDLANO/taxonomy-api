package no.ndla.taxonomy.repositories;


import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.domain.TopicSubtopic;

import java.util.List;

public interface TopicSubtopicRepository extends TaxonomyRepository<TopicSubtopic> {
    List<TopicSubtopic> findByTopic(Topic topic);
}