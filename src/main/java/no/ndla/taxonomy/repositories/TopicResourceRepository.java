package no.ndla.taxonomy.repositories;


import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.domain.TopicResource;

import java.util.List;

public interface TopicResourceRepository extends TaxonomyRepository<TopicResource> {
    List<TopicResource> findByTopic(Topic topic);
}