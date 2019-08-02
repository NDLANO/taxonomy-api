package no.ndla.taxonomy.repositories;


import no.ndla.taxonomy.domain.Subject;
import no.ndla.taxonomy.domain.SubjectTopic;
import org.springframework.data.jpa.repository.Query;

import java.net.URI;
import java.util.Collection;
import java.util.List;

public interface SubjectTopicRepository extends TaxonomyRepository<SubjectTopic> {
    List<SubjectTopic> findBySubject(Subject s);

    @Query("SELECT st" +
            "   FROM SubjectTopic st" +
            "   JOIN FETCH st.subject" +
            "   JOIN FETCH st.topic")
    List<SubjectTopic> findAllIncludingSubjectAndTopic();

    @Query("SELECT DISTINCT st" +
            "   FROM SubjectTopic st" +
            "   JOIN FETCH st.topic t" +
            "   JOIN FETCH st.subject s" +
            "   LEFT JOIN FETCH s.cachedUrls" +
            "   WHERE t.publicId = :topicPublicId")
    List<SubjectTopic> findAllByTopicPublicIdIncludingSubjectAndTopicAndCachedUrls(URI topicPublicId);

    @Query("SELECT DISTINCT st" +
            "   FROM SubjectTopic st" +
            "   LEFT JOIN FETCH st.topic t" +
            "   LEFT JOIN FETCH st.subject" +
            "   LEFT JOIN FETCH t.topicFilters tf" +
            "   LEFT JOIN FETCH tf.filter f" +
            "   LEFT JOIN FETCH tf.relevance" +
            "   LEFT JOIN FETCH t.cachedUrls" +
            "   LEFT JOIN FETCH t.translations" +
            "   LEFT JOIN FETCH f.translations" +
            "   WHERE st.subject = :subject AND st.topic.id IN :topicId")
    List<SubjectTopic> findAllBySubjectAndTopicId(Subject subject, Collection<Integer> topicId);


}
