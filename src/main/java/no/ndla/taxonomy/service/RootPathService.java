package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Resource;
import no.ndla.taxonomy.domain.Subject;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.repositories.ResourceRepository;
import no.ndla.taxonomy.repositories.SubjectRepository;
import no.ndla.taxonomy.repositories.TopicRepository;

import java.net.URI;
import java.util.Optional;

public abstract class RootPathService implements PathComponentGeneratingService {
    private SubjectRepository subjectRepository;
    private TopicRepository topicRepository;
    private ResourceRepository resourceRepository;

    protected RootPathService(SubjectRepository subjectRepository, TopicRepository topicRepository, ResourceRepository resourceRepository) {
        this.subjectRepository = subjectRepository;
        this.topicRepository = topicRepository;
        this.resourceRepository = resourceRepository;
    }

    public abstract String subjectPath();
    public abstract String topicPath();
    public abstract String resourcePath();

    public Optional<String> generateRootPath(URI root) {
        Subject subject = subjectRepository.findByPublicId(root);
        if (subject != null) {
            return Optional.of(subjectPath() + "/" + generatePathComponent(subject));
        }
        Topic topic = topicRepository.findByPublicId(root);
        if (topic != null) {
            return Optional.of(topicPath() + "/" + generatePathComponent(topic));
        }
        Resource resource = resourceRepository.findByPublicId(root);
        if (resource != null) {
            return Optional.of(resourcePath() + "/" + generatePathComponent(resource));
        }
        return Optional.empty();
    }
}
