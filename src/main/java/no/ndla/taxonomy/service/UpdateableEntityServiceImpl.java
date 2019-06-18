package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.ResolvablePathEntity;
import no.ndla.taxonomy.domain.ResolvablePathEntityView;
import no.ndla.taxonomy.repositories.ResourceRepository;
import no.ndla.taxonomy.repositories.SubjectRepository;
import no.ndla.taxonomy.repositories.TopicRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UpdateableEntityServiceImpl implements UpdateableEntityService {
    private SubjectRepository subjectRepository;
    private TopicRepository topicRepository;
    private ResourceRepository resourceRepository;

    UpdateableEntityServiceImpl(SubjectRepository subjectRepository, TopicRepository topicRepository, ResourceRepository resourceRepository) {
        this.subjectRepository = subjectRepository;
        this.topicRepository = topicRepository;
        this.resourceRepository = resourceRepository;
    }

    @Override
    public Optional<ResolvablePathEntity> getUpdateableEntity(ResolvablePathEntityView updateableEntityView) {
        switch (updateableEntityView.getEntityType()) {
            case "resource":
                return resourceRepository.findById(updateableEntityView.getEntityId()).map(updateableEntity -> updateableEntity);
            case "topic":
                return topicRepository.findById(updateableEntityView.getEntityId()).map(updateableEntity -> updateableEntity);
            case "subject":
                return subjectRepository.findById(updateableEntityView.getEntityId()).map(updateableEntity -> updateableEntity);
        }

        return Optional.empty();
    }
}
