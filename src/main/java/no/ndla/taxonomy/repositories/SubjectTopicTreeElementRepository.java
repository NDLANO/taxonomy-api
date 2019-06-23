package no.ndla.taxonomy.repositories;

import no.ndla.taxonomy.domain.SubjectTopicTreeElement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubjectTopicTreeElementRepository extends JpaRepository<SubjectTopicTreeElement, Integer> {
    List<SubjectTopicTreeElement> findAllBySubjectIdOrderBySubjectIdAscParentTopicIdAscTopicRankAsc(int subjectId);
}
