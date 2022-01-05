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
    private CachedPathRepository cachedPathRepository;
    private CachedUrlUpdaterServiceImpl service;

    private NodeRepository nodeRepository;
    private ResourceRepository resourceRepository;
    private VersionRepository versionRepository;

    @BeforeEach
    void setup(@Autowired CachedPathRepository cachedPathRepository, @Autowired NodeRepository nodeRepository,
            @Autowired ResourceRepository resourceRepository, @Autowired VersionRepository versionRepository) {
        this.cachedPathRepository = cachedPathRepository;
        this.nodeRepository = nodeRepository;
        this.resourceRepository = resourceRepository;
        this.versionRepository = versionRepository;

        service = new CachedUrlUpdaterServiceImpl(cachedPathRepository);
    }

    @Test
    @Transactional
    void updateCachedUrls() {
        final var subject1 = new Node(NodeType.SUBJECT, new Version());
        subject1.setPublicId(URI.create("urn:subject:1"));
        subject1.setContext(true);

        nodeRepository.save(subject1);

        service.updateCachedUrls(subject1);

        {
            assertEquals(1, subject1.getCachedPaths().size());
            final var path1 = subject1.getCachedPaths().iterator().next();
            assertEquals("/subject:1", path1.getPath());
            assertSame(subject1, path1.getNode().orElseThrow());
            assertTrue(path1.isPrimary());

            assertEquals(1, cachedPathRepository.findAllByPublicId(URI.create("urn:subject:1")).size());
        }

        final var topic1 = new Node(NodeType.TOPIC, new Version());
        topic1.setPublicId(URI.create("urn:topic:1"));
        topic1.setContext(true);

        nodeRepository.save(topic1);

        service.updateCachedUrls(topic1);

        {
            assertEquals(1, topic1.getCachedPaths().size());
            final var path1 = topic1.getCachedPaths().iterator().next();
            assertEquals("/topic:1", path1.getPath());
            assertSame(topic1, path1.getNode().orElseThrow());
            assertTrue(path1.isPrimary());

            assertEquals(1, cachedPathRepository.findAllByPublicId(URI.create("urn:topic:1")).size());
        }

        topic1.addParentConnection(NodeConnection.create(subject1, topic1));

        service.updateCachedUrls(topic1);

        {
            assertEquals(2, cachedPathRepository.findAllByPublicId(URI.create("urn:topic:1")).size());
            assertTrue(
                    cachedPathRepository.findAllByPublicId(URI.create("urn:topic:1")).stream().map(CachedPath::getPath)
                            .collect(Collectors.toList()).containsAll(Set.of("/topic:1", "/subject:1/topic:1")));
        }

        topic1.setContext(false);

        service.updateCachedUrls(topic1);
        {
            assertEquals(1, cachedPathRepository.findAllByPublicId(URI.create("urn:topic:1")).size());
            assertTrue(cachedPathRepository.findAllByPublicId(URI.create("urn:topic:1")).stream()
                    .map(CachedPath::getPath).collect(Collectors.toList()).contains("/subject:1/topic:1"));
        }

        final var topic2 = new Node(NodeType.TOPIC, new Version());
        topic2.setPublicId(URI.create("urn:topic:2"));
        nodeRepository.save(topic2);

        service.updateCachedUrls(topic1);

        {
            assertEquals(0, cachedPathRepository.findAllByPublicId(URI.create("urn:topic:2")).size());
        }

        topic1.addChildConnection(NodeConnection.create(topic1, topic2));

        service.updateCachedUrls(topic1);

        {
            assertEquals(1, cachedPathRepository.findAllByPublicId(URI.create("urn:topic:2")).size());
            assertTrue(cachedPathRepository.findAllByPublicId(URI.create("urn:topic:2")).stream()
                    .map(CachedPath::getPath).collect(Collectors.toList()).contains("/subject:1/topic:1/topic:2"));
        }

        topic1.setContext(true);

        service.updateCachedUrls(topic1);

        {
            assertEquals(2, cachedPathRepository.findAllByPublicId(URI.create("urn:topic:2")).size());
            assertTrue(cachedPathRepository.findAllByPublicId(URI.create("urn:topic:2")).stream()
                    .map(CachedPath::getPath).collect(Collectors.toList())
                    .containsAll(Set.of("/subject:1/topic:1/topic:2", "/topic:1/topic:2")));
        }

        final var resource1 = new Resource(versionRepository.findFirstByVersionType(VersionType.BETA));
        resource1.setPublicId(URI.create("urn:resource:1"));
        resourceRepository.save(resource1);

        service.updateCachedUrls(resource1);

        {
            assertEquals(0, cachedPathRepository.findAllByPublicId(URI.create("urn:resource:1")).size());
        }

        topic1.addNodeResource(NodeResource.create(topic1, resource1));

        service.updateCachedUrls(resource1);

        {
            assertEquals(2, cachedPathRepository.findAllByPublicId(URI.create("urn:resource:1")).size());
            assertTrue(cachedPathRepository.findAllByPublicId(URI.create("urn:resource:1")).stream()
                    .map(CachedPath::getPath).collect(Collectors.toList())
                    .containsAll(Set.of("/subject:1/topic:1/resource:1", "/topic:1/resource:1")));
        }

        topic2.addNodeResource(NodeResource.create(topic2, resource1));

        service.updateCachedUrls(resource1);

        {
            assertEquals(4, cachedPathRepository.findAllByPublicId(URI.create("urn:resource:1")).size());
            assertTrue(cachedPathRepository.findAllByPublicId(URI.create("urn:resource:1")).stream()
                    .map(CachedPath::getPath).collect(Collectors.toList())
                    .containsAll(Set.of("/subject:1/topic:1/resource:1", "/topic:1/resource:1",
                            "/subject:1/topic:1/topic:2/resource:1", "/topic:1/topic:2/resource:1")));
        }

        resourceRepository.delete(resource1);

        {
            assertEquals(0, cachedPathRepository.findAllByPublicId(URI.create("urn:resource:1")).size());
        }
    }

    @Test
    void clearCachedUrls() {
        final var subject1 = new Node(NodeType.SUBJECT, new Version());
        subject1.setPublicId(URI.create("urn:subject:1"));
        subject1.setContext(true);

        nodeRepository.save(subject1);

        service.updateCachedUrls(subject1);

        assertEquals(1, subject1.getCachedPaths().size());

        service.clearCachedUrls(subject1);

        assertEquals(1, subject1.getCachedPaths().size());
        assertEquals(0,
                subject1.getCachedPaths().stream().filter(CachedPath::isActive).collect(Collectors.toSet()).size());
    }
}
