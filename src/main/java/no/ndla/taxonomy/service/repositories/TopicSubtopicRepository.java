package no.ndla.taxonomy.service.repositories;


import no.ndla.taxonomy.service.domain.Topic;
import no.ndla.taxonomy.service.domain.TopicSubtopic;

import java.util.List;

public interface TopicSubtopicRepository extends TaxonomyRepository<TopicSubtopic> {
    List<TopicSubtopic> findByTopic(Topic topic);
}
