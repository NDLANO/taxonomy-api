/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import static no.ndla.taxonomy.TestUtils.assertAnyTrue;
import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.repositories.ChangelogRepository;
import no.ndla.taxonomy.repositories.NodeConnectionRepository;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.repositories.VersionRepository;
import no.ndla.taxonomy.rest.v1.commands.VersionPostPut;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Will only be run in maven using '-P integration'
 */
@SpringBootTest
public class NodePublishingIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    VersionRepository versionRepository;

    @Autowired
    NodeRepository nodeRepository;

    @Autowired
    NodeConnectionRepository nodeConnectionRepository;

    @Autowired
    NodeService nodeService;

    @Autowired
    NodeConnectionService nodeConnectionService;

    @Autowired
    VersionService versionService;

    @Autowired
    ChangelogRepository changelogRepository;

    @Autowired
    Builder builder;

    @Autowired
    DomainEntityHelperService domainEntityHelperService;

    @Autowired
    ThreadPoolTaskExecutor executor;

    @BeforeEach
    void clearAllRepos() {
        VersionContext.setCurrentVersion(versionService.schemaFromHash(null));
        versionRepository.deleteAllAndFlush();
        nodeConnectionRepository.deleteAllAndFlush();
        nodeRepository.deleteAllAndFlush();
        changelogRepository.deleteAll();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void can_publish_node_to_schema() throws Exception {
        final var command = new VersionPostPut() {
            {
                name = "Beta";
            }
        };
        Version target = versionService.createNewVersion(Optional.empty(), command);
        assertTrue(checkSchemaExists(versionService.schemaFromHash(target.getHash())));
        executor.getThreadPoolExecutor().awaitTermination(2, TimeUnit.SECONDS);

        // a node to make sure testnode is not the first
        builder.node(n -> n.publicId("urn:node:first").resource(r -> r.publicId("urn:resource:first")));
        Node node = builder.node(n -> n.publicId("urn:node:1")
                .name("Node")
                .grepCode("KM123")
                .customField("key", "value")
                .isContext(true)
                .translation("NN Node", "nn")
                .translation("NB Node", "nb"));

        TestTransaction.flagForCommit();
        TestTransaction.end();

        nodeService.publishNode(node.getPublicId(), Optional.empty(), target.getPublicId(), true, false);

        while (!changelogRepository.findAll().isEmpty()) {
            executor.getThreadPoolExecutor().awaitTermination(2, TimeUnit.SECONDS);
        }

        VersionContext.setCurrentVersion(versionService.schemaFromHash(target.getHash()));
        Node published = nodeRepository.findFirstByPublicId(node.getPublicId()).get();
        VersionContext.setCurrentVersion(versionService.schemaFromHash(null));

        assertNotNull(published);
        assertNotEquals(node.getId(), published.getId()); // node should have 1, published 3
        assertEquals(node.getName(), published.getName());
        assertEquals(node.getNodeType(), published.getNodeType());
        assertEquals(1, published.getContexts().size());
        assertEquals(node.getMetadata().isVisible(), published.getMetadata().isVisible());
        assertEquals(
                node.getMetadata().getGrepCodes().size(),
                published.getMetadata().getGrepCodes().size());
        assertAnyTrue(published.getMetadata().getGrepCodes(), grepCode -> grepCode.getCode()
                .equals("KM123"));
        assertEquals(node.getCustomFields().size(), published.getCustomFields().size());
        assertAnyTrue(
                published.getCustomFields().entrySet(),
                customFieldValue -> customFieldValue.getKey().equals("key"));
        assertAnyTrue(
                published.getCustomFields().entrySet(),
                customFieldValue -> customFieldValue.getValue().equals("value"));
        assertNotNull(published.getTranslations());
        assertAnyTrue(
                published.getTranslations(),
                translation -> translation.getName().contains("NN Node"));
        assertAnyTrue(
                published.getTranslations(),
                translation -> translation.getName().contains("NB Node"));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void can_publish_node_with_resource_to_schema() throws Exception {
        final var command = new VersionPostPut() {
            {
                name = "Beta";
            }
        };
        Version target = versionService.createNewVersion(Optional.empty(), command);
        assertTrue(checkSchemaExists(versionService.schemaFromHash(target.getHash())));
        executor.getThreadPoolExecutor().awaitTermination(2, TimeUnit.SECONDS);

        // a node to make sure testnode is not the first
        builder.node(n -> n.resource(r -> r.publicId("urn:resource:first")));
        Node node = builder.node(n -> n.publicId("urn:node:1")
                .grepCode("KM123")
                .customField("key", "value")
                .resource(r -> r.publicId("urn:resource:1")
                        .name("Resource")
                        .grepCode("KM234")
                        .customField("key2", "value2")
                        .translation("nb", tr -> tr.name("Resource NB"))
                        .translation("nn", tr -> tr.name("Resource NN"))));

        TestTransaction.flagForCommit();
        TestTransaction.end();

        nodeService.publishNode(node.getPublicId(), Optional.empty(), target.getPublicId(), true, false);

        while (!changelogRepository.findAll().isEmpty()) {
            executor.getThreadPoolExecutor().awaitTermination(2, TimeUnit.SECONDS);
        }

        VersionContext.setCurrentVersion(versionService.schemaFromHash(target.getHash()));
        Node published = (Node) domainEntityHelperService
                .getProcessedEntityByPublicId(node.getPublicId(), false, false)
                .get();
        Node connected = published.getResourceChildren().stream()
                .findFirst()
                .get()
                .getResource()
                .get();
        Node resource =
                nodeRepository.findFirstByPublicId(connected.getPublicId()).get();
        VersionContext.setCurrentVersion(versionService.schemaFromHash(null));

        assertEquals(
                node.getResourceChildren().size(),
                published.getResourceChildren().size());
        assertNotEquals(node.getId(), published.getId());
        assertEquals("urn:resource:1", connected.getPublicId().toString());
        assertEquals("Resource", connected.getName());
        assertAnyTrue(connected.getMetadata().getGrepCodes(), grepCode -> grepCode.getCode()
                .equals("KM234"));
        assertAnyTrue(
                connected.getCustomFields().entrySet(),
                customFieldValue -> customFieldValue.getKey().equals("key2"));
        assertAnyTrue(
                connected.getCustomFields().entrySet(),
                customFieldValue -> customFieldValue.getValue().equals("value2"));
        assertAnyTrue(
                resource.getTranslations(), translation -> translation.getName().contains("Resource NB"));
        assertAnyTrue(
                resource.getTranslations(), translation -> translation.getName().contains("Resource NN"));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void can_publish_sub_node_with_resource_to_schema() throws Exception {
        final var command = new VersionPostPut() {
            {
                name = "Beta";
            }
        };
        Version target = versionService.createNewVersion(Optional.empty(), command);
        assertTrue(checkSchemaExists(versionService.schemaFromHash(target.getHash())));
        executor.getThreadPoolExecutor().awaitTermination(2, TimeUnit.SECONDS);

        // a node to make sure testnode is not the first
        builder.node(n -> n.publicId("urn:node:first").resource(r -> r.publicId("urn:resource:first")));
        Node node = builder.node(
                NodeType.SUBJECT,
                s -> s.isContext(true).publicId("urn:subject:1").child(NodeType.TOPIC, t -> t.publicId("urn:topic:1")
                        .resource(r ->
                                r.publicId("urn:resource:1").name("Resource").isVisible(false))));

        TestTransaction.flagForCommit();
        TestTransaction.end();

        nodeService.publishNode(node.getPublicId(), Optional.empty(), target.getPublicId(), true, false);

        while (!changelogRepository.findAll().isEmpty()) {
            executor.getThreadPoolExecutor().awaitTermination(2, TimeUnit.SECONDS);
        }

        VersionContext.setCurrentVersion(versionService.schemaFromHash(target.getHash()));
        Optional<Node> resource = nodeRepository.findFirstByPublicId(URI.create("urn:resource:1"));
        VersionContext.setCurrentVersion(versionService.schemaFromHash(null));

        assertTrue(resource.isPresent());
        assertFalse(resource.get().getMetadata().isVisible());
        assertEquals("Resource", resource.get().getName());
        assertAnyTrue(resource.get().getAllPaths(), path -> path.equals("/subject:1/topic:1/resource:1"));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void can_publish_node_tree_with_resources_to_schema() throws Exception {
        final var command = new VersionPostPut() {
            {
                name = "Beta";
            }
        };
        Version target = versionService.createNewVersion(Optional.empty(), command);
        assertTrue(checkSchemaExists(versionService.schemaFromHash(target.getHash())));
        executor.getThreadPoolExecutor().awaitTermination(2, TimeUnit.SECONDS);

        // a node to make sure testnode is not the first
        builder.node(n -> n.publicId("urn:node:first").resource(r -> r.publicId("urn:resource:first")));
        Node node = builder.node(NodeType.SUBJECT, s -> s.isContext(true)
                .publicId("urn:subject:1")
                .child(NodeType.TOPIC, t -> t.publicId("urn:topic:1").resource(r -> r.publicId("urn:resource:1")))
                .child(NodeType.TOPIC, t2 -> t2.publicId("urn:topic:2")
                        .resource(r2 -> r2.publicId("urn:resource:2"))
                        .child(
                                NodeType.TOPIC,
                                t3 -> t3.publicId("urn:topic:3").grepCode("TT2").isVisible(false))));

        TestTransaction.flagForCommit();
        TestTransaction.end();

        nodeService.publishNode(node.getPublicId(), Optional.empty(), target.getPublicId(), true, false);

        while (!changelogRepository.findAll().isEmpty()) {
            executor.getThreadPoolExecutor().awaitTermination(2, TimeUnit.SECONDS);
        }

        VersionContext.setCurrentVersion(versionService.schemaFromHash(target.getHash()));
        Node resource = (Node) domainEntityHelperService
                .getProcessedEntityByPublicId(URI.create("urn:resource:1"), false, false)
                .get();
        Node subnode = (Node) domainEntityHelperService
                .getProcessedEntityByPublicId(URI.create("urn:topic:2"), false, false)
                .get();
        Node subsubnode = (Node) domainEntityHelperService
                .getProcessedEntityByPublicId(URI.create("urn:topic:3"), false, false)
                .get();
        VersionContext.setCurrentVersion(versionService.schemaFromHash(null));

        assertNotNull(resource);
        assertNotNull(subnode);
        assertFalse(subnode.getResourceChildren().isEmpty());
        assertNotNull(subsubnode);
        assertAnyTrue(subsubnode.getMetadata().getGrepCodes(), grepCode -> grepCode.getCode()
                .equals("TT2"));
        assertAnyTrue(resource.getAllPaths(), path -> path.equals("/subject:1/topic:1/resource:1"));
        assertAnyTrue(subnode.getAllPaths(), path -> path.equals("/subject:1/topic:2"));
        assertAnyTrue(subsubnode.getAllPaths(), path -> path.equals("/subject:1/topic:2/topic:3"));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void can_publish_node_tree_with_reused_resource_to_schema() throws Exception {
        final var command = new VersionPostPut() {
            {
                name = "Beta";
            }
        };
        Version target = versionService.createNewVersion(Optional.empty(), command);
        assertTrue(checkSchemaExists(versionService.schemaFromHash(target.getHash())));
        executor.getThreadPoolExecutor().awaitTermination(2, TimeUnit.SECONDS);

        // nodes to make sure testnode is not the first
        builder.node(n -> n.publicId("urn:node:first").resource(r -> r.publicId("urn:resource:first")));
        builder.node(n -> n.publicId("urn:node:second").resource(r -> r.publicId("urn:resource:second")));
        builder.node(n -> n.publicId("urn:node:third").resource(r -> r.publicId("urn:resource:third")));
        Node resource = builder.node(NodeType.RESOURCE, r -> r.publicId("urn:resource:1"));
        Node node = builder.node(NodeType.SUBJECT, s -> s.isContext(true)
                .publicId("urn:subject:1")
                .child(NodeType.TOPIC, t -> t.publicId("urn:topic:1").resource(resource))
                .child(NodeType.TOPIC, t2 -> t2.publicId("urn:topic:2")
                        .resource(resource)
                        .resource(r2 -> r2.publicId("urn:resource:2"))
                        .child(
                                NodeType.TOPIC,
                                t3 -> t3.publicId("urn:topic:3").grepCode("TT2").isVisible(false))));

        TestTransaction.flagForCommit();
        TestTransaction.end();

        nodeService.publishNode(node.getPublicId(), Optional.empty(), target.getPublicId(), true, false);

        while (!changelogRepository.findAll().isEmpty()) {
            executor.getThreadPoolExecutor().awaitTermination(2, TimeUnit.SECONDS);
        }

        VersionContext.setCurrentVersion(versionService.schemaFromHash(target.getHash()));
        Node updated = (Node) domainEntityHelperService
                .getProcessedEntityByPublicId(URI.create("urn:resource:1"), false, false)
                .get();
        VersionContext.setCurrentVersion(versionService.schemaFromHash(null));

        assertNotNull(updated);
        assertNotEquals(updated.getId(), resource.getId());
        assertNotNull(updated.getContexts());
        assertEquals(2, updated.getParentConnections().size()); // Should be used twice
        assertAnyTrue(
                updated.getParentConnections(),
                nodeResource -> nodeResource.getParent().get().getPublicId().equals(URI.create("urn:topic:1")));
        assertAnyTrue(
                updated.getParentConnections(),
                nodeResource -> nodeResource.getParent().get().getPublicId().equals(URI.create("urn:topic:2")));
        assertAnyTrue(updated.getAllPaths(), path -> path.equals("/subject:1/topic:1/resource:1"));
        assertAnyTrue(updated.getAllPaths(), path -> path.equals("/subject:1/topic:2/resource:1"));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void can_publish_node_in_tree_to_schema() throws Exception {
        final var command = new VersionPostPut() {
            {
                name = "Beta";
            }
        };
        Version target = versionService.createNewVersion(Optional.empty(), command);
        assertTrue(checkSchemaExists(versionService.schemaFromHash(target.getHash())));
        executor.getThreadPoolExecutor().awaitTermination(2, TimeUnit.SECONDS);

        // a node to make sure testnode is not the first
        builder.node(n -> n.publicId("urn:node:first").resource(r -> r.publicId("urn:resource:first")));
        Node child = builder.node(n -> n.publicId("urn:node:1")
                .isContext(true)
                .isVisible(false)
                .resource(r2 -> r2.publicId("urn:resource:2")));
        Node node = builder.node(NodeType.SUBJECT, s -> s.isContext(true)
                .publicId("urn:subject:1")
                .child(NodeType.TOPIC, t -> t.publicId("urn:topic:1"))
                .child(child)
                .child(NodeType.TOPIC, t3 -> t3.publicId("urn:topic:3").isVisible(false)));

        TestTransaction.flagForCommit();
        TestTransaction.end();

        nodeService.publishNode(node.getPublicId(), Optional.empty(), target.getPublicId(), true, false);

        while (!changelogRepository.findAll().isEmpty()) {
            executor.getThreadPoolExecutor().awaitTermination(2, TimeUnit.SECONDS);
        }

        VersionContext.setCurrentVersion(versionService.schemaFromHash(target.getHash()));
        Node updatedChild = (Node) domainEntityHelperService
                .getProcessedEntityByPublicId(child.getPublicId(), false, false)
                .get();
        VersionContext.setCurrentVersion(versionService.schemaFromHash(null));

        assertNotNull(updatedChild);
        assertFalse(updatedChild.getMetadata().isVisible());
        assertFalse(updatedChild.getResourceChildren().isEmpty());
        assertFalse(updatedChild.getParentNodes().isEmpty());
        assertEquals(
                node.getPublicId(),
                updatedChild.getParentNodes().stream().findFirst().get().getPublicId());
        assertAnyTrue(updatedChild.getAllPaths(), path -> path.equals("/subject:1/node:1"));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void can_publish_node_tree_with_reused_resource_twice_to_schema() throws Exception {
        final var command = new VersionPostPut() {
            {
                name = "Beta";
            }
        };
        Version target = versionService.createNewVersion(Optional.empty(), command);
        assertTrue(checkSchemaExists(versionService.schemaFromHash(target.getHash())));
        executor.getThreadPoolExecutor().awaitTermination(2, TimeUnit.SECONDS);

        // a node to make sure testnode is not the first
        builder.node(n -> n.publicId("urn:node:first").resource(r -> r.publicId("urn:resource:first")));
        Node resource = builder.node(NodeType.RESOURCE, r -> r.publicId("urn:resource:1"));
        Node node = builder.node(NodeType.SUBJECT, s -> s.isContext(true)
                .publicId("urn:subject:1")
                .child(NodeType.TOPIC, t2 -> t2.publicId("urn:topic:1").resource(resource)));
        Node second = builder.node(NodeType.SUBJECT, s -> s.isContext(true)
                .publicId("urn:subject:2")
                .child(NodeType.TOPIC, t -> t.publicId("urn:topic:2").resource(resource)));

        TestTransaction.flagForCommit();
        TestTransaction.end();

        nodeService.publishNode(node.getPublicId(), Optional.empty(), target.getPublicId(), true, false);
        nodeService.publishNode(second.getPublicId(), Optional.empty(), target.getPublicId(), true, false);

        while (!changelogRepository.findAll().isEmpty()) {
            executor.getThreadPoolExecutor().awaitTermination(2, TimeUnit.SECONDS);
        }

        VersionContext.setCurrentVersion(versionService.schemaFromHash(target.getHash()));
        Node updated = (Node) domainEntityHelperService
                .getProcessedEntityByPublicId(URI.create("urn:resource:1"), false, false)
                .get();
        VersionContext.setCurrentVersion(versionService.schemaFromHash(null));

        assertNotNull(updated);
        assertNotNull(updated.getContexts());
        assertEquals(2, updated.getParentConnections().size()); // Should be used twice
        assertAnyTrue(
                updated.getParentConnections(),
                nodeResource -> nodeResource.getParent().get().getPublicId().equals(URI.create("urn:topic:1")));
        assertAnyTrue(
                updated.getParentConnections(),
                nodeResource -> nodeResource.getParent().get().getPublicId().equals(URI.create("urn:topic:2")));
        assertAnyTrue(updated.getAllPaths(), path -> path.equals("/subject:1/topic:1/resource:1"));
        assertAnyTrue(updated.getAllPaths(), path -> path.equals("/subject:2/topic:2/resource:1"));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void can_publish_reused_resource_with_translations_to_schema() throws Exception {
        final var command = new VersionPostPut() {
            {
                name = "Beta";
            }
        };
        Version target = versionService.createNewVersion(Optional.empty(), command);
        assertTrue(checkSchemaExists(versionService.schemaFromHash(target.getHash())));
        executor.getThreadPoolExecutor().awaitTermination(2, TimeUnit.SECONDS);

        // a node to make sure testnode is not the first
        builder.node(n -> n.publicId("urn:node:first").resource(r -> r.publicId("urn:resource:first")));
        builder.resourceType(rt -> rt.name("Fagstoff")
                .translation("nb", t -> t.name("Fagstoff nb"))
                .translation("nn", t -> t.name("Fagstoff nn"))
                .subtype("urn:resourcetype:1", st -> st.name("Fagartikkel")
                        .publicId("urn:resourcetype:1")
                        .translation("nb", t -> t.name("Fagartikkel nb"))
                        .translation("nn", t -> t.name("Fagartikkel nn"))));
        Node resource = builder.node(NodeType.RESOURCE, r -> r.publicId("urn:resource:1")
                .resourceType("urn:resourcetype:1")
                .translation("nb", tr -> tr.name("Resource nb"))
                .translation("nn", tr -> tr.name("Resource nn")));
        Node node = builder.node(NodeType.SUBJECT, s -> s.isContext(true)
                .publicId("urn:subject:1")
                .child(NodeType.TOPIC, t2 -> t2.publicId("urn:topic:1").resource(resource)));
        Node second = builder.node(NodeType.SUBJECT, s -> s.isContext(true)
                .publicId("urn:subject:2")
                .child(NodeType.TOPIC, t -> t.publicId("urn:topic:2").resource(resource)));

        TestTransaction.flagForCommit();
        TestTransaction.end();

        nodeService.publishNode(node.getPublicId(), Optional.empty(), target.getPublicId(), true, false);

        while (!changelogRepository.findAll().isEmpty()) {
            executor.getThreadPoolExecutor().awaitTermination(2, TimeUnit.SECONDS);
        }

        // Update translations to resource in standard schema for updating
        TestTransaction.start();
        TestTransaction.flagForCommit();
        Node r = nodeRepository.findByPublicId(resource.getPublicId());
        var nb = r.getTranslation("nb");
        // Update
        nb.get().setName("Resource nb updated");
        // Add new
        var resourceTranslation = new JsonTranslation("en");
        resourceTranslation.setName("Resource en");
        r.addTranslation(resourceTranslation);
        // Remove
        r.removeTranslation("nn");
        entityManager.persist(r);
        TestTransaction.end();

        nodeService.publishNode(second.getPublicId(), Optional.empty(), target.getPublicId(), true, false);

        while (!changelogRepository.findAll().isEmpty()) {
            executor.getThreadPoolExecutor().awaitTermination(2, TimeUnit.SECONDS);
        }

        VersionContext.setCurrentVersion(versionService.schemaFromHash(target.getHash()));
        Node updated = (Node) domainEntityHelperService
                .getProcessedEntityByPublicId(URI.create("urn:resource:1"), false, false)
                .get();
        VersionContext.setCurrentVersion(versionService.schemaFromHash(null));

        assertNotNull(updated);
        assertNotNull(updated.getContexts());
        assertEquals(2, updated.getParentConnections().size()); // Should be used twice
        assertAnyTrue(
                updated.getParentConnections(),
                nodeResource -> nodeResource.getParent().get().getPublicId().equals(URI.create("urn:topic:1")));
        assertAnyTrue(
                updated.getParentConnections(),
                nodeResource -> nodeResource.getParent().get().getPublicId().equals(URI.create("urn:topic:2")));
        assertAnyTrue(updated.getAllPaths(), path -> path.equals("/subject:1/topic:1/resource:1"));
        assertAnyTrue(updated.getAllPaths(), path -> path.equals("/subject:2/topic:2/resource:1"));

        assertNotNull(updated.getResourceTypes());
        assertEquals(1, updated.getResourceTypes().size());

        assertEquals(2, updated.getTranslations().size());
        assertAnyTrue(
                updated.getTranslations(), translation -> translation.getName().equals("Resource nb updated"));
        assertAnyTrue(
                updated.getTranslations(), translation -> translation.getName().equals("Resource en"));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void can_publish_node_twice_to_schema() throws Exception {
        final var command = new VersionPostPut() {
            {
                name = "Beta";
            }
        };
        Version target = versionService.createNewVersion(Optional.empty(), command);
        assertTrue(checkSchemaExists(versionService.schemaFromHash(target.getHash())));
        executor.getThreadPoolExecutor().awaitTermination(2, TimeUnit.SECONDS);

        // a node to make sure testnode is not the first
        builder.node(n -> n.publicId("urn:node:first").resource(r -> r.publicId("urn:resource:first")));
        Node node = builder.node(n -> n.name("Node")
                .isContext(true)
                .translation("nn", tr -> tr.name("NN Node"))
                .translation("nb", tr -> tr.name("NB Node")));

        TestTransaction.flagForCommit();
        TestTransaction.end();

        nodeService.publishNode(node.getPublicId(), Optional.empty(), target.getPublicId(), true, false);

        while (!changelogRepository.findAll().isEmpty()) {
            executor.getThreadPoolExecutor().awaitTermination(2, TimeUnit.SECONDS);
        }

        // Update translations to node in standard schema for updating
        TestTransaction.start();
        TestTransaction.flagForCommit();
        Node n = nodeRepository.findByPublicId(node.getPublicId());
        var nb = n.getTranslation("nb");
        // Update
        nb.get().setName("NB Node updated");
        // Add new
        n.addTranslation("EN Node", "en");
        // Remove
        n.removeTranslation("nn");
        entityManager.persist(n);
        TestTransaction.end();

        nodeService.publishNode(node.getPublicId(), Optional.empty(), target.getPublicId(), true, false);

        while (!changelogRepository.findAll().isEmpty()) {
            executor.getThreadPoolExecutor().awaitTermination(1, TimeUnit.SECONDS);
        }

        VersionContext.setCurrentVersion(versionService.schemaFromHash(target.getHash()));
        Node published = nodeRepository.findFirstByPublicId(node.getPublicId()).get();
        VersionContext.setCurrentVersion(versionService.schemaFromHash(null));

        assertNotNull(published);
        assertEquals(2, published.getTranslations().size());
        assertNotEquals(node.getId(), published.getId());
        assertAnyTrue(
                published.getTranslations(),
                translation -> translation.getName().contains("EN Node"));
        assertAnyTrue(
                published.getTranslations(),
                translation -> translation.getName().contains("NB Node updated"));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void can_publish_node_tree_to_schema_async() throws Exception {
        final var command = new VersionPostPut() {
            {
                name = "Beta";
            }
        };
        Version target = versionService.createNewVersion(Optional.empty(), command);
        assertTrue(checkSchemaExists(versionService.schemaFromHash(target.getHash())));
        executor.getThreadPoolExecutor().awaitTermination(2, TimeUnit.SECONDS);

        List<Changelog> changelogs = changelogRepository.findAll();
        assertTrue(changelogs.isEmpty());

        // a node to make sure testnode is not the first
        builder.node(n -> n.publicId("urn:node:first").resource(r -> r.publicId("urn:resource:first")));
        Node node = builder.node(n -> n.publicId("urn:node:1")
                .name("Node")
                .isContext(true)
                .translation("nn", tr -> tr.name("NN Node"))
                .translation("nb", tr -> tr.name("NB Node"))
                .customField(CustomField.IS_CHANGED, "true")
                .customField("to be kept", "true")
                .child(c -> c.publicId("urn:node:2"))
                .resource(r -> r.publicId("urn:resource:1")));

        TestTransaction.flagForCommit();
        TestTransaction.end();

        nodeService.publishNode(node.getPublicId(), Optional.empty(), target.getPublicId(), true, false);

        while (!changelogRepository.findAll().isEmpty()) {
            executor.getThreadPoolExecutor().awaitTermination(2, TimeUnit.SECONDS);
        }

        // Check node is published to schema
        VersionContext.setCurrentVersion(versionService.schemaFromHash(target.getHash()));
        Node published = (Node) domainEntityHelperService
                .getProcessedEntityByPublicId(node.getPublicId(), false, false)
                .get();
        VersionContext.setCurrentVersion(versionService.schemaFromHash(null));

        assertNotNull(published);
        assertEquals(2, published.getTranslations().size());
        assertEquals(2, published.getChildNodes().size());
        assertEquals(1, published.getResources().size());
        var customfields = new ArrayList<>(published.getCustomFields().keySet());
        assertEquals(1, customfields.size());
        assertAnyTrue(customfields, customfield -> customfield.equals("to be kept"));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void can_publish_node_tree_with_moved_resource_to_schema() throws Exception {
        final var command = new VersionPostPut() {
            {
                name = "Beta";
            }
        };
        Version target = versionService.createNewVersion(Optional.empty(), command);
        assertTrue(checkSchemaExists(versionService.schemaFromHash(target.getHash())));
        executor.getThreadPoolExecutor().awaitTermination(2, TimeUnit.SECONDS);

        // a node to make sure testnode is not the first
        builder.node(n -> n.publicId("urn:node:first").resource(r -> r.publicId("urn:resource:first")));
        Node resource = builder.node(NodeType.RESOURCE, r -> r.publicId("urn:resource:1"));
        Node topic1 =
                builder.node(NodeType.TOPIC, t -> t.publicId("urn:topic:1").resource(resource));
        Node topic2 = builder.node(NodeType.TOPIC, t -> t.publicId("urn:topic:2"));
        Node node = builder.node(
                NodeType.SUBJECT,
                s -> s.isContext(true).publicId("urn:subject:1").child(topic1).child(topic2));

        TestTransaction.flagForCommit();
        TestTransaction.end();

        nodeService.publishNode(node.getPublicId(), Optional.empty(), target.getPublicId(), true, false);

        while (!changelogRepository.findAll().isEmpty()) {
            executor.getThreadPoolExecutor().awaitTermination(2, TimeUnit.SECONDS);
        }

        TestTransaction.start();
        TestTransaction.flagForCommit();
        // Move resource to another node
        nodeConnectionService.disconnectParentChild(topic1, resource);
        nodeConnectionService.connectParentChild(topic2, resource, builder.core(), null, Optional.empty());
        TestTransaction.end();

        nodeService.publishNode(node.getPublicId(), Optional.empty(), target.getPublicId(), true, false);

        while (!changelogRepository.findAll().isEmpty()) {
            executor.getThreadPoolExecutor().awaitTermination(2, TimeUnit.SECONDS);
        }

        VersionContext.setCurrentVersion(versionService.schemaFromHash(target.getHash()));
        Node updated = (Node) domainEntityHelperService
                .getProcessedEntityByPublicId(URI.create("urn:resource:1"), false, false)
                .get();
        VersionContext.setCurrentVersion(versionService.schemaFromHash(null));

        assertNotNull(updated);
        assertNotNull(updated.getContexts());
        assertEquals(1, updated.getParentConnections().size());
        assertAnyTrue(
                updated.getParentConnections(),
                nodeResource -> nodeResource.getParent().get().getPublicId().equals(URI.create("urn:topic:2")));
        assertAnyTrue(updated.getAllPaths(), path -> path.equals("/subject:1/topic:2/resource:1"));
    }
}
