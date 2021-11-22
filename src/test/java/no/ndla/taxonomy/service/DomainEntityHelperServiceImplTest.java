/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@Transactional
class DomainEntityHelperServiceImplTest {
    private Node subject1;
    private Node subject2;
    private Node topic1;
    private Node topic2;

    private DomainEntityHelperServiceImpl service;

    @BeforeEach
    void setUp(@Autowired NodeRepository nodeRepository) {
        service = new DomainEntityHelperServiceImpl(nodeRepository);

        topic1 = new Node(NodeType.TOPIC);
        topic1.setPublicId(URI.create("urn:topic:dehsit:1"));
        topic1 = nodeRepository.save(topic1);

        topic2 = new Node(NodeType.TOPIC);
        topic2.setPublicId(URI.create("urn:topic:dehsit:2"));
        topic2 = nodeRepository.save(topic2);

        subject1 = new Node(NodeType.SUBJECT);
        ;
        subject1.setPublicId(URI.create("urn:subject:dehsit:1"));
        subject1 = nodeRepository.save(subject1);

        subject2 = new Node(NodeType.SUBJECT);
        ;
        subject2.setPublicId(URI.create("urn:subject:dehsit:2"));
        subject2 = nodeRepository.save(subject2);
    }

    @Test
    void getSubjectByPublicId() {
        assertSame(subject1, service.getNodeByPublicId(URI.create("urn:subject:dehsit:1")));
        assertSame(subject2, service.getNodeByPublicId(URI.create("urn:subject:dehsit:2")));

        assertThrows(NotFoundServiceException.class, () -> service.getNodeByPublicId(URI.create("urn:topic:dehsit:3")));
    }

    @Test
    void getTopicByPublicId() {
        assertSame(topic1, service.getNodeByPublicId(URI.create("urn:topic:dehsit:1")));
        assertSame(topic2, service.getNodeByPublicId(URI.create("urn:topic:dehsit:2")));

        assertThrows(NotFoundServiceException.class, () -> service.getNodeByPublicId(URI.create("urn:topic:dehsit:3")));
    }
}
