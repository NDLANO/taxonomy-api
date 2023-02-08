/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ExtendWith(SpringExtension.class)
class CachedUrlUpdaterServiceImplTest {
    private CachedUrlUpdaterServiceImpl service;

    private NodeRepository nodeRepository;
    private NodeConnectionRepository nodeConnectionRepository;

    @BeforeEach
    void setup(@Autowired NodeRepository nodeRepository, @Autowired NodeConnectionRepository nodeConnectionRepository) {
        this.nodeRepository = nodeRepository;
        this.nodeConnectionRepository = nodeConnectionRepository;

        service = new CachedUrlUpdaterServiceImpl(nodeRepository);
    }

    @Test
    @Transactional
    void updateCachedUrls() {
        final var subject1 = new Node(NodeType.SUBJECT);
        subject1.setPublicId(URI.create("urn:subject:1"));
        subject1.setContext(true);

        nodeRepository.save(subject1);

        service.updateCachedUrls(subject1);

        {
            assertEquals(1, subject1.getCachedPaths().size());
            final var path1 = subject1.getCachedPaths().iterator().next();
            assertEquals("/subject:1", path1.getPath());
            assertEquals("urn:subject:1", path1.getPublicId().toString());
            assertTrue(path1.isPrimary());

            var node = nodeRepository.findFirstByPublicId(URI.create("urn:subject:1"));
            assertEquals(1, node.get().getCachedPaths().size());
        }

        final var topic1 = new Node(NodeType.TOPIC);
        topic1.setPublicId(URI.create("urn:topic:1"));
        topic1.setContext(true);

        nodeRepository.save(topic1);

        service.updateCachedUrls(topic1);

        {
            assertEquals(1, topic1.getCachedPaths().size());
            final var path1 = topic1.getCachedPaths().iterator().next();
            assertEquals("/topic:1", path1.getPath());
            assertEquals("urn:topic:1", path1.getPublicId().toString());
            assertTrue(path1.isPrimary());

            var node = nodeRepository.findFirstByPublicId(URI.create("urn:topic:1"));
            assertEquals(1, node.get().getCachedPaths().size());
        }

        var nc = NodeConnection.create(subject1, topic1);
        nodeConnectionRepository.save(nc);
        topic1.addParentConnection(nc);

        service.updateCachedUrls(topic1);

        {
            var node = nodeRepository.findFirstByPublicId(URI.create("urn:topic:1")).get();
            assertEquals(2, node.getCachedPaths().size());
            assertTrue(node.getCachedPaths().stream().map(CachedPath::getPath).toList()
                    .containsAll(Set.of("/topic:1", "/subject:1/topic:1")));
        }

        topic1.setContext(false);

        service.updateCachedUrls(topic1);
        {
            var node = nodeRepository.findFirstByPublicId(URI.create("urn:topic:1")).get();
            assertEquals(1, node.getCachedPaths().size());
            assertTrue(node.getCachedPaths().stream().map(CachedPath::getPath).toList().contains("/subject:1/topic:1"));
        }

        final var topic2 = new Node(NodeType.TOPIC);
        topic2.setPublicId(URI.create("urn:topic:2"));
        nodeRepository.save(topic2);

        service.updateCachedUrls(topic1);

        {
            var node = nodeRepository.findFirstByPublicId(URI.create("urn:topic:2")).get();
            assertEquals(0, node.getCachedPaths().size());
        }

        var nc2 = NodeConnection.create(topic1, topic2);
        topic1.addChildConnection(nc2);
        nodeConnectionRepository.save(nc2);

        service.updateCachedUrls(topic1);

        {
            var node = nodeRepository.findFirstByPublicId(URI.create("urn:topic:2")).get();
            assertEquals(1, node.getCachedPaths().size());
            assertTrue(node.getCachedPaths().stream().map(CachedPath::getPath).toList()
                    .contains("/subject:1/topic:1/topic:2"));
        }

        topic1.setContext(true);

        service.updateCachedUrls(topic1);

        {
            var node = nodeRepository.findFirstByPublicId(URI.create("urn:topic:2")).get();
            assertEquals(2, node.getCachedPaths().size());
            assertTrue(node.getCachedPaths().stream().map(CachedPath::getPath).toList()
                    .containsAll(Set.of("/subject:1/topic:1/topic:2", "/topic:1/topic:2")));
        }

        final var resource1 = new Node(NodeType.RESOURCE);
        resource1.setPublicId(URI.create("urn:resource:1"));
        nodeRepository.save(resource1);

        service.updateCachedUrls(resource1);

        {
            var node = nodeRepository.findFirstByPublicId(URI.create("urn:resource:1")).get();
            assertEquals(0, node.getCachedPaths().size());
        }

        var nc3 = NodeConnection.create(topic1, resource1);
        nodeConnectionRepository.save(nc3);
        topic1.addChildConnection(nc3);

        service.updateCachedUrls(resource1);

        {
            var node = nodeRepository.findFirstByPublicId(URI.create("urn:resource:1")).get();
            assertEquals(2, node.getCachedPaths().size());
            assertTrue(node.getCachedPaths().stream().map(CachedPath::getPath).toList()
                    .containsAll(Set.of("/subject:1/topic:1/resource:1", "/topic:1/resource:1")));
        }

        var nc4 = NodeConnection.create(topic2, resource1);
        nodeConnectionRepository.save(nc4);
        topic2.addChildConnection(nc4);

        service.updateCachedUrls(resource1);

        {
            var node = nodeRepository.findFirstByPublicId(URI.create("urn:resource:1")).get();
            assertEquals(4, node.getCachedPaths().size());
            assertTrue(node.getCachedPaths().stream().map(CachedPath::getPath).toList()
                    .containsAll(Set.of("/subject:1/topic:1/resource:1", "/topic:1/resource:1",
                            "/subject:1/topic:1/topic:2/resource:1", "/topic:1/topic:2/resource:1")));
        }

        nodeRepository.delete(resource1);

        {
            var node = nodeRepository.findFirstByPublicId(URI.create("urn:resource:1"));
            assertTrue(node.isEmpty());
        }
    }

    @Test
    void clearCachedUrls() {
        final var subject1 = new Node(NodeType.SUBJECT);
        subject1.setPublicId(URI.create("urn:subject:1"));
        subject1.setContext(true);

        nodeRepository.save(subject1);

        service.updateCachedUrls(subject1);

        assertEquals(1, subject1.getCachedPaths().size());

        service.clearCachedUrls(subject1);

        assertEquals(0, subject1.getCachedPaths().size());
    }
}
