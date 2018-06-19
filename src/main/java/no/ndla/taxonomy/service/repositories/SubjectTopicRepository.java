package no.ndla.taxonomy.service.repositories;


import no.ndla.taxonomy.service.domain.Subject;
import no.ndla.taxonomy.service.domain.SubjectTopic;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.net.URI;
import java.util.List;

public interface SubjectTopicRepository extends TaxonomyRepository<SubjectTopic> {
    List<SubjectTopic> findBySubject(Subject s);

}
