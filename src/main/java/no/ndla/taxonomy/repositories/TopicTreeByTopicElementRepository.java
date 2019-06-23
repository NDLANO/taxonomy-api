package no.ndla.taxonomy.repositories;

import no.ndla.taxonomy.domain.TopicTreeByTopicElement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TopicTreeByTopicElementRepository extends JpaRepository<TopicTreeByTopicElement, String> {
    List<TopicTreeByTopicElement> findAllByRootTopicIdOrTopicIdOrderByParentTopicIdAscParentTopicIdAscTopicRankAsc(int rootTopicId, int topicId);
}
