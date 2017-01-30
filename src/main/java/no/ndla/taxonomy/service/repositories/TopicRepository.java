package no.ndla.taxonomy.service.repositories;


import no.ndla.taxonomy.service.domain.Topic;

import java.net.URI;
import java.util.List;

public interface TopicRepository extends TaxonomyRepository<Topic> {
    List<Topic> getBySubjectTopicsSubjectPublicId(URI id);
}
