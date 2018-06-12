package no.ndla.taxonomy.service.repositories;


import no.ndla.taxonomy.service.domain.SubjectTopic;

import java.net.URI;
import java.util.List;

public interface SubjectTopicRepository extends TaxonomyRepository<SubjectTopic> {
    public List<SubjectTopic> findBySubjectPublicIdOrderByRank(URI publicId);
}
