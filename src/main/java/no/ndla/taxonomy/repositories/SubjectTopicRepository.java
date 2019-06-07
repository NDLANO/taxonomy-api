package no.ndla.taxonomy.repositories;


import no.ndla.taxonomy.domain.Subject;
import no.ndla.taxonomy.domain.SubjectTopic;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SubjectTopicRepository extends TaxonomyRepository<SubjectTopic> {
    List<SubjectTopic> findBySubject(Subject s);

    @Query("SELECT st" +
            "   FROM SubjectTopic st" +
            "   JOIN FETCH st.subject" +
            "   JOIN FETCH st.topic")
    List<SubjectTopic> findAllIncludingSubjectAndTopic();
}
