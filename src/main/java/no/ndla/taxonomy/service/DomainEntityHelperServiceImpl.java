/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.Subject;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.repositories.SubjectRepository;
import no.ndla.taxonomy.repositories.TopicRepository;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;

@Component
public class DomainEntityHelperServiceImpl implements DomainEntityHelperService {
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final NodeRepository nodeRepository;

    public DomainEntityHelperServiceImpl(SubjectRepository subjectRepository, TopicRepository topicRepository, NodeRepository nodeRepository) {
        this.subjectRepository = subjectRepository;
        this.topicRepository = topicRepository;
        this.nodeRepository = nodeRepository;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Subject getSubjectByPublicId(URI publicId) {
        return subjectRepository.findFirstByPublicId(publicId).orElseThrow(() -> new NotFoundServiceException("Subject", publicId));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Topic getTopicByPublicId(URI publicId) {
        return topicRepository.findFirstByPublicId(publicId).orElseThrow(() -> new NotFoundServiceException("Topic", publicId));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Node getNodeByPublicId(URI publicId) {
        return nodeRepository.findFirstByPublicId(publicId).orElseThrow(() -> new NotFoundServiceException("Node", publicId));
    }

}
