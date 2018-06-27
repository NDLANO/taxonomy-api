package no.ndla.taxonomy.service.repositories;


import no.ndla.taxonomy.service.domain.Topic;
import no.ndla.taxonomy.service.domain.TopicResource;

import java.util.List;

public interface TopicResourceRepository extends TaxonomyRepository<TopicResource> {
    List<TopicResource> findByTopic(Topic topic);
}
