/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.repositories;

import no.ndla.taxonomy.domain.Subject;
import no.ndla.taxonomy.domain.SubjectTopic;
import no.ndla.taxonomy.domain.Topic;
import org.springframework.data.jpa.repository.Query;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SubjectTopicRepository extends TaxonomyRepository<SubjectTopic> {
    List<SubjectTopic> findBySubject(Subject s);

    @Query("SELECT st" + "   FROM SubjectTopic st" + "   JOIN FETCH st.subject" + "   JOIN FETCH st.topic")
    List<SubjectTopic> findAllIncludingSubjectAndTopic();

    @Query("SELECT DISTINCT st" + "   FROM SubjectTopic st" + "   JOIN FETCH st.topic t" + "   JOIN FETCH st.subject s"
            + "   LEFT JOIN FETCH s.cachedPaths" + "   WHERE t.publicId = :topicPublicId")
    List<SubjectTopic> findAllByTopicPublicIdIncludingSubjectAndTopicAndCachedUrls(URI topicPublicId);

    @Query("SELECT DISTINCT st" + "   FROM SubjectTopic st" + "   LEFT JOIN FETCH st.topic t"
            + "   LEFT JOIN FETCH st.subject" + "   LEFT JOIN FETCH st.relevance" + "   LEFT JOIN FETCH t.cachedPaths"
            + "   LEFT JOIN FETCH t.translations" + "   WHERE st.subject = :subject AND st.topic.id IN :topicId")
    List<SubjectTopic> doFindAllBySubjectAndTopicId(Subject subject, Collection<Integer> topicId);

    default List<SubjectTopic> findAllBySubjectAndTopicId(Subject subject, Collection<Integer> topicId) {
        if (topicId.size() == 0) {
            return doFindAllBySubjectAndTopicId(subject, null);
        }

        return doFindAllBySubjectAndTopicId(subject, topicId);
    }

    Optional<SubjectTopic> findFirstBySubjectAndTopic(Subject subject, Topic topic);
}
