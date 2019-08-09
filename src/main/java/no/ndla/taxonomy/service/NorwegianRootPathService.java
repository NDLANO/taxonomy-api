package no.ndla.taxonomy.service;

import no.ndla.taxonomy.repositories.ResourceRepository;
import no.ndla.taxonomy.repositories.SubjectRepository;
import no.ndla.taxonomy.repositories.TopicRepository;
import org.springframework.stereotype.Service;

@Service
public class NorwegianRootPathService extends RootPathService {
    public NorwegianRootPathService(SubjectRepository subjectRepository, TopicRepository topicRepository, ResourceRepository resourceRepository) {
        super(subjectRepository, topicRepository, resourceRepository);
    }

    @Override
    public String subjectPath() {
        return "fag";
    }

    @Override
    public String topicPath() {
        return "emne";
    }

    @Override
    public String resourcePath() {
        return "ressurs";
    }
}
