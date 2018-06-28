package no.ndla.taxonomy.repositories;


import no.ndla.taxonomy.domain.Subject;
import no.ndla.taxonomy.domain.SubjectTopic;

import java.util.List;

public interface SubjectTopicRepository extends TaxonomyRepository<SubjectTopic> {
    List<SubjectTopic> findBySubject(Subject s);
}
