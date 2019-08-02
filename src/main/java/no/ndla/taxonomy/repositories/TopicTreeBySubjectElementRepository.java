package no.ndla.taxonomy.repositories;

import no.ndla.taxonomy.domain.TopicTreeBySubjectElement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TopicTreeBySubjectElementRepository extends JpaRepository<TopicTreeBySubjectElement, Integer> {
    List<TopicTreeBySubjectElement> findAllBySubjectIdOrderBySubjectIdAscParentTopicIdAscTopicRankAsc(int subjectId);
}
