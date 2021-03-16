package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Subject;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.repositories.TopicRepository;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;

@Component
public class DomainEntityHelperServiceImpl implements DomainEntityHelperService {
    private final TopicRepository topicRepository;

    public DomainEntityHelperServiceImpl(TopicRepository topicRepository) {
        this.topicRepository = topicRepository;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Topic getSubjectByPublicId(URI publicId) {
        return topicRepository.findFirstByPublicId(publicId).orElseThrow(() -> new NotFoundServiceException("Subject", publicId));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Topic getTopicByPublicId(URI publicId) {
        return topicRepository.findFirstByPublicId(publicId).orElseThrow(() -> new NotFoundServiceException("Topic", publicId));
    }

}
