/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.Set;
import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.repositories.NodeConnectionRepository;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.repositories.RelevanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ExtendWith(SpringExtension.class)
class ContextUpdaterServiceImplTest extends AbstractIntegrationTest {
    private ContextUpdaterServiceImpl service;

    private RelevanceRepository relevanceRepository;
    private NodeRepository nodeRepository;
    private NodeConnectionRepository nodeConnectionRepository;

    @BeforeEach
    void setup(
            @Autowired RelevanceRepository relevanceRepository,
            @Autowired NodeRepository nodeRepository,
            @Autowired NodeConnectionRepository nodeConnectionRepository) {
        this.relevanceRepository = relevanceRepository;
        this.nodeRepository = nodeRepository;
        this.nodeConnectionRepository = nodeConnectionRepository;

        service = new ContextUpdaterServiceImpl();
    }

    @Value(value = "${new.url.separator:false}")
    private boolean newUrlSeparator;

    @Test
    @Transactional
    void updateCachedUrls() {
        final var core = new Relevance();
        core.setName("core");
        core.setPublicId(URI.create("uri:relevance:core"));
        relevanceRepository.save(core);

        final var subject1 = new Node(NodeType.SUBJECT);
        subject1.setPublicId(URI.create("urn:subject:1"));
        subject1.setName("Subject1");
        subject1.setContext(true);

        nodeRepository.save(subject1);

        service.updateContexts(subject1);

        {
            assertEquals(1, subject1.getContexts().size());
            final var context = subject1.getContexts().iterator().next();
            assertEquals("/subject:1", context.path());
            assertEquals("urn:subject:1", context.rootId());
            assertEquals(0, context.breadcrumbs().size());
            assertTrue(context.isPrimary());
            if (newUrlSeparator) {
                assertTrue(context.url().isPresent() && context.url().get().startsWith("/subject1/f/"));
            } else {
                assertTrue(context.url().isPresent() && context.url().get().startsWith("/subject1__"));
            }

            var node = nodeRepository.findFirstByPublicId(URI.create("urn:subject:1"));
            assertEquals(1, node.get().getContexts().size());
        }

        final var topic1 = new Node(NodeType.TOPIC);
        topic1.setPublicId(URI.create("urn:topic:1"));
        topic1.setName("Topic1");
        topic1.setContext(true);

        nodeRepository.save(topic1);

        service.updateContexts(topic1);

        {
            assertEquals(1, topic1.getContexts().size());
            final var context = topic1.getContexts().iterator().next();
            assertEquals("/topic:1", context.path());
            assertEquals("urn:topic:1", context.rootId().toString());
            assertEquals(0, context.breadcrumbs().size());
            assertTrue(context.isPrimary());
            if (newUrlSeparator) {
                assertTrue(context.url().isPresent() && context.url().get().startsWith("/topic1/e/"));
            } else {
                assertTrue(context.url().isPresent() && context.url().get().startsWith("/topic1__"));
            }

            var node = nodeRepository.findFirstByPublicId(URI.create("urn:topic:1"));
            assertEquals(1, node.get().getContexts().size());
        }

        var nc = NodeConnection.create(subject1, topic1, core);
        nodeConnectionRepository.save(nc);
        topic1.addParentConnection(nc);

        service.updateContexts(topic1);

        {
            var node = nodeRepository
                    .findFirstByPublicId(URI.create("urn:topic:1"))
                    .get();
            assertEquals(2, node.getContexts().size());
            assertTrue(node.getContexts().stream()
                    .map(TaxonomyContext::path)
                    .toList()
                    .containsAll(Set.of("/topic:1", "/subject:1/topic:1")));
        }

        topic1.setContext(false);

        service.updateContexts(topic1);

        {
            var node = nodeRepository
                    .findFirstByPublicId(URI.create("urn:topic:1"))
                    .get();
            assertEquals(1, node.getContexts().size());
            assertTrue(node.getContexts().stream()
                    .map(TaxonomyContext::path)
                    .toList()
                    .contains("/subject:1/topic:1"));
        }

        final var topic2 = new Node(NodeType.TOPIC);
        topic2.setPublicId(URI.create("urn:topic:2"));
        topic2.setName("Topic2");
        nodeRepository.save(topic2);

        service.updateContexts(topic1);

        {
            var node = nodeRepository
                    .findFirstByPublicId(URI.create("urn:topic:2"))
                    .get();
            assertEquals(0, node.getContexts().size());
        }

        var nc2 = NodeConnection.create(topic1, topic2, core);
        topic1.addChildConnection(nc2);
        nodeConnectionRepository.save(nc2);

        service.updateContexts(topic1);

        {
            var node = nodeRepository
                    .findFirstByPublicId(URI.create("urn:topic:2"))
                    .get();
            assertEquals(1, node.getContexts().size());
            assertTrue(node.getContexts().stream()
                    .map(TaxonomyContext::path)
                    .toList()
                    .contains("/subject:1/topic:1/topic:2"));
        }

        topic1.setContext(true);

        service.updateContexts(topic1);

        {
            var node = nodeRepository
                    .findFirstByPublicId(URI.create("urn:topic:2"))
                    .get();
            assertEquals(2, node.getContexts().size());
            assertTrue(node.getContexts().stream()
                    .map(TaxonomyContext::path)
                    .toList()
                    .containsAll(Set.of("/subject:1/topic:1/topic:2", "/topic:1/topic:2")));
        }

        final var resource1 = new Node(NodeType.RESOURCE);
        resource1.setPublicId(URI.create("urn:resource:1"));
        nodeRepository.save(resource1);

        service.updateContexts(resource1);

        {
            var node = nodeRepository
                    .findFirstByPublicId(URI.create("urn:resource:1"))
                    .get();
            assertEquals(0, node.getContexts().size());
        }

        var nc3 = NodeConnection.create(topic1, resource1, core);
        nodeConnectionRepository.save(nc3);
        topic1.addChildConnection(nc3);

        service.updateContexts(resource1);

        {
            var node = nodeRepository
                    .findFirstByPublicId(URI.create("urn:resource:1"))
                    .get();
            assertEquals(2, node.getContexts().size());
            assertTrue(node.getContexts().stream()
                    .map(TaxonomyContext::path)
                    .toList()
                    .containsAll(Set.of("/subject:1/topic:1/resource:1", "/topic:1/resource:1")));
        }

        var nc4 = NodeConnection.create(topic2, resource1, core);
        nodeConnectionRepository.save(nc4);
        topic2.addChildConnection(nc4);

        service.updateContexts(resource1);

        {
            var node = nodeRepository
                    .findFirstByPublicId(URI.create("urn:resource:1"))
                    .get();
            assertEquals(4, node.getContexts().size());
            assertTrue(node.getContexts().stream()
                    .map(TaxonomyContext::path)
                    .toList()
                    .containsAll(Set.of(
                            "/subject:1/topic:1/resource:1",
                            "/topic:1/resource:1",
                            "/subject:1/topic:1/topic:2/resource:1",
                            "/topic:1/topic:2/resource:1")));
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

        service.updateContexts(subject1);

        assertEquals(1, subject1.getContexts().size());

        service.clearContexts(subject1);

        assertEquals(0, subject1.getContexts().size());
    }
}
