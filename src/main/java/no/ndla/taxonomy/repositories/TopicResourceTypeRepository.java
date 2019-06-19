package no.ndla.taxonomy.repositories;

import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.domain.TopicResourceType;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TopicResourceTypeRepository extends TaxonomyRepository<TopicResourceType> {
    @Query( "SELECT obj" +
            " FROM TopicResourceType obj" +
            " JOIN FETCH obj.topic" +
            " JOIN FETCH obj.resourceType")
    List<TopicResourceType> findAll();

    @Query( "SELECT obj" +
            " FROM TopicResourceType obj" +
            " JOIN FETCH obj.resourceType rt" +
            " LEFT JOIN FETCH rt.parent" +
            " LEFT JOIN FETCH rt.resourceTypeTranslations" +
            " WHERE obj.topic = :topic")
    List<TopicResourceType> findAllByTopic(Topic topic);
}
