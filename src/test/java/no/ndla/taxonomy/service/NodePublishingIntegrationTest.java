/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.repositories.*;
import no.ndla.taxonomy.rest.v1.commands.VersionCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.Optional;

import static no.ndla.taxonomy.TestUtils.assertAnyTrue;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Must be run separately in IDE, excluded from mvn build because of shared database state.
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
    ResourceRepository resourceRepository;

    @Autowired
    NodeResourceRepository nodeResourceRepository;

    @Autowired
    GrepCodeRepository grepCodeRepository;

    @Autowired
    NodeService nodeService;

    @Autowired
    VersionService versionService;

    @Autowired
    Builder builder;

    @BeforeEach
    void clearAllRepos() {
        VersionContext.setCurrentVersion(versionService.schemaFromHash(null));
        versionRepository.deleteAllAndFlush();
        nodeRepository.deleteAllAndFlush();
        nodeConnectionRepository.deleteAllAndFlush();
        resourceRepository.deleteAllAndFlush();
        nodeResourceRepository.deleteAllAndFlush();
        grepCodeRepository.deleteAll();
    }

    @Test
    @Transactional
    void can_publish_node_to_schema() {
        final var command = new VersionCommand() {
            {
                name = "Beta";
            }
        };
        Version target = versionService.createNewVersion(Optional.empty(), command);
        assertTrue(checkSchemaExists(versionService.schemaFromHash(target.getHash())));

        // some nodes to make sure testnode is not the first
        builder.node();
        builder.node();
        Node node = builder.node(
                n -> n.publicId("urn:node:1").name("Node").grepCode("KM123").customField("key", "value").isContext(true)
                        .translation("nn", tr -> tr.name("NN Node")).translation("nb", tr -> tr.name("NB Node")));

        TestTransaction.flagForCommit();
        TestTransaction.end();

        nodeService.publishNode(node.getPublicId(), Optional.empty(), target.getPublicId());

        VersionContext.setCurrentVersion(versionService.schemaFromHash(target.getHash()));
        Node published = nodeRepository.findFirstByPublicIdIncludingCachedUrlsAndTranslations(node.getPublicId()).get();
        VersionContext.setCurrentVersion(versionService.schemaFromHash(null));

        assertNotNull(published);
        assertNotEquals(node.getId(), published.getId());// node should have 1, published 3
        assertEquals(node.getName(), published.getName());
        assertEquals(node.getNodeType(), published.getNodeType());
        assertNotNull(published.getCachedPaths());
        assertEquals(1, published.getCachedPaths().size());
        assertNotNull(published.getMetadata());
        assertEquals(node.getMetadata().isVisible(), published.getMetadata().isVisible());
        assertEquals(node.getMetadata().getGrepCodes().size(), published.getMetadata().getGrepCodes().size());
        assertAnyTrue(published.getMetadata().getGrepCodes(), grepCode -> grepCode.getCode().equals("KM123"));
        assertEquals(node.getMetadata().getCustomFieldValues().size(),
                published.getMetadata().getCustomFieldValues().size());
        assertAnyTrue(published.getMetadata().getCustomFieldValues(),
                customFieldValue -> customFieldValue.getCustomField().getKey().equals("key"));
        assertAnyTrue(published.getMetadata().getCustomFieldValues(),
                customFieldValue -> customFieldValue.getValue().equals("value"));
        assertNotNull(published.getTranslations());
        assertAnyTrue(published.getTranslations(), translation -> translation.getName().contains("NN Node"));
        assertAnyTrue(published.getTranslations(), translation -> translation.getName().contains("NB Node"));
    }

    @Test
    @Transactional
    void can_publish_node_with_resource_to_schema() {
        final var command = new VersionCommand() {
            {
                name = "Beta";
            }
        };
        Version target = versionService.createNewVersion(Optional.empty(), command);
        assertTrue(checkSchemaExists(versionService.schemaFromHash(target.getHash())));

        // some nodes to make sure testnode is not the first
        builder.node();
        builder.node();
        Node node = builder.node(n -> n.publicId("urn:node:1").grepCode("KM123").customField("key", "value")
                .resource(r -> r.publicId("urn:resource:1").name("Resource").grepCode("KM234")
                        .customField("key2", "value2").translation("nb", tr -> tr.name("Resource NB"))
                        .translation("nn", tr -> tr.name("Resource NN"))));

        TestTransaction.flagForCommit();
        TestTransaction.end();

        nodeService.publishNode(node.getPublicId(), Optional.empty(), target.getPublicId());

        VersionContext.setCurrentVersion(versionService.schemaFromHash(target.getHash()));
        Node published = nodeRepository.fetchNodeGraphByPublicId(node.getPublicId()).get();
        Resource connected = published.getNodeResources().stream().findFirst().get().getResource().get();
        Resource resource = resourceRepository
                .findFirstByPublicIdIncludingCachedUrlsAndTranslations(connected.getPublicId()).get();
        VersionContext.setCurrentVersion(versionService.schemaFromHash(null));

        assertEquals(node.getNodeResources().size(), published.getNodeResources().size());
        assertNotEquals(node.getId(), published.getId());
        assertEquals("urn:resource:1", connected.getPublicId().toString());
        assertEquals("Resource", connected.getName());
        assertNotNull(connected.getMetadata());
        assertAnyTrue(connected.getMetadata().getGrepCodes(), grepCode -> grepCode.getCode().equals("KM234"));
        assertAnyTrue(connected.getMetadata().getCustomFieldValues(),
                customFieldValue -> customFieldValue.getCustomField().getKey().equals("key2"));
        assertAnyTrue(connected.getMetadata().getCustomFieldValues(),
                customFieldValue -> customFieldValue.getValue().equals("value2"));
        assertNotNull(resource.getTranslations());
        assertAnyTrue(resource.getTranslations(), translation -> translation.getName().contains("Resource NB"));
        assertAnyTrue(resource.getTranslations(), translation -> translation.getName().contains("Resource NN"));
    }

    @Test
    @Transactional
    void can_publish_node_tree_to_schema() {
        final var command = new VersionCommand() {
            {
                name = "Beta";
            }
        };
        Version target = versionService.createNewVersion(Optional.empty(), command);
        assertTrue(checkSchemaExists(versionService.schemaFromHash(target.getHash())));

        Node node = builder.node(NodeType.SUBJECT,
                s -> s.isContext(true).publicId("urn:subject:1").child(NodeType.TOPIC,
                        t -> t.publicId("urn:topic:1").child(NodeType.TOPIC, st -> st.publicId("urn:topic:2"))));

        TestTransaction.flagForCommit();
        TestTransaction.end();

        nodeService.publishNode(node.getPublicId(), Optional.empty(), target.getPublicId());

        VersionContext.setCurrentVersion(versionService.schemaFromHash(target.getHash()));
        Node published = nodeRepository.fetchNodeGraphByPublicId(URI.create("urn:topic:1")).get();
        VersionContext.setCurrentVersion(versionService.schemaFromHash(null));

        assertFalse(published.isContext());
        assertFalse(published.isRoot());
        assertNotNull(published.getMetadata());
        assertNotNull(published.getChildren());
        assertAnyTrue(published.getChildren(), nodeConnection -> nodeConnection.getChild().isPresent());
        assertEquals(URI.create("urn:topic:2"),
                published.getChildren().stream().findFirst().get().getChild().get().getPublicId());
    }

    @Test
    @Transactional
    void can_publish_sub_node_with_resource_to_schema() {
        final var command = new VersionCommand() {
            {
                name = "Beta";
            }
        };
        Version target = versionService.createNewVersion(Optional.empty(), command);
        assertTrue(checkSchemaExists(versionService.schemaFromHash(target.getHash())));

        Node node = builder.node(NodeType.SUBJECT,
                s -> s.isContext(true).publicId("urn:subject:1").child(NodeType.TOPIC, t -> t.publicId("urn:topic:1")
                        .resource(r -> r.publicId("urn:resource:1").name("Resource").isVisible(false))));

        TestTransaction.flagForCommit();
        TestTransaction.end();

        nodeService.publishNode(node.getPublicId(), Optional.empty(), target.getPublicId());

        VersionContext.setCurrentVersion(versionService.schemaFromHash(target.getHash()));
        Optional<Resource> resource = resourceRepository
                .findFirstByPublicIdIncludingCachedUrlsAndTranslations(URI.create("urn:resource:1"));
        VersionContext.setCurrentVersion(versionService.schemaFromHash(null));

        assertTrue(resource.isPresent());
        assertNotNull(resource.get().getMetadata());
        assertFalse(resource.get().getMetadata().isVisible());
        assertEquals("Resource", resource.get().getName());
        assertAnyTrue(resource.get().getAllPaths(), path -> path.equals("/subject:1/topic:1/resource:1"));
    }

    @Test
    @Transactional
    void can_publish_node_tree_with_resources_to_schema() {
        final var command = new VersionCommand() {
            {
                name = "Beta";
            }
        };
        Version target = versionService.createNewVersion(Optional.empty(), command);
        assertTrue(checkSchemaExists(versionService.schemaFromHash(target.getHash())));

        Node node = builder.node(NodeType.SUBJECT, s -> s.isContext(true).publicId("urn:subject:1")
                .child(NodeType.TOPIC, t -> t.publicId("urn:topic:1").resource(r -> r.publicId("urn:resource:1")))
                .child(NodeType.TOPIC, t2 -> t2.publicId("urn:topic:2").resource(r2 -> r2.publicId("urn:resource:2"))
                        .child(NodeType.TOPIC, t3 -> t3.publicId("urn:topic:3").grepCode("TT2").isVisible(false))));

        TestTransaction.flagForCommit();
        TestTransaction.end();

        nodeService.publishNode(node.getPublicId(), Optional.empty(), target.getPublicId());

        VersionContext.setCurrentVersion(versionService.schemaFromHash(target.getHash()));
        Optional<Resource> resource = resourceRepository.fetchResourceGraphByPublicId(URI.create("urn:resource:1"));
        Optional<Node> subnode = nodeRepository.fetchNodeGraphByPublicId(URI.create("urn:topic:2"));
        Optional<Node> subsubnode = nodeRepository.fetchNodeGraphByPublicId(URI.create("urn:topic:3"));
        VersionContext.setCurrentVersion(versionService.schemaFromHash(null));

        assertTrue(resource.isPresent());
        assertTrue(subnode.isPresent());
        assertFalse(subnode.get().getNodeResources().isEmpty());
        assertTrue(subsubnode.isPresent());
        assertNotNull(subsubnode.get().getMetadata());
        assertAnyTrue(subsubnode.get().getMetadata().getGrepCodes(), grepCode -> grepCode.getCode().equals("TT2"));
        assertAnyTrue(resource.get().getAllPaths(), path -> path.equals("/subject:1/topic:1/resource:1"));
        assertAnyTrue(subnode.get().getAllPaths(), path -> path.equals("/subject:1/topic:2"));
        assertAnyTrue(subsubnode.get().getAllPaths(), path -> path.equals("/subject:1/topic:2/topic:3"));
    }

    @Test
    @Transactional
    void can_publish_node_tree_with_reused_resource_to_schema() {
        final var command = new VersionCommand() {
            {
                name = "Beta";
            }
        };
        Version target = versionService.createNewVersion(Optional.empty(), command);
        assertTrue(checkSchemaExists(versionService.schemaFromHash(target.getHash())));

        Resource resource = builder.resource(r -> r.publicId("urn:resource:1"));
        Node node = builder
                .node(NodeType.SUBJECT,
                        s -> s.isContext(true).publicId("urn:subject:1")
                                .child(NodeType.TOPIC, t -> t.publicId("urn:topic:1").resource(resource))
                                .child(NodeType.TOPIC, t2 -> t2.publicId("urn:topic:2").resource(resource)
                                        .resource(r2 -> r2.publicId("urn:resource:2")).child(NodeType.TOPIC,
                                                t3 -> t3.publicId("urn:topic:3").grepCode("TT2").isVisible(false))));

        TestTransaction.flagForCommit();
        TestTransaction.end();

        nodeService.publishNode(node.getPublicId(), Optional.empty(), target.getPublicId());

        VersionContext.setCurrentVersion(versionService.schemaFromHash(target.getHash()));
        Optional<Resource> updated = resourceRepository.fetchResourceGraphByPublicId(URI.create("urn:resource:1"));
        VersionContext.setCurrentVersion(versionService.schemaFromHash(null));

        assertTrue(updated.isPresent());
        assertNotNull(updated.get().getCachedPaths());
        assertFalse(updated.get().getNodeResources().isEmpty());
        assertEquals(2, updated.get().getNodeResources().size()); // Should be used twice
        assertAnyTrue(updated.get().getNodeResources(),
                nodeResource -> nodeResource.getNode().get().getPublicId().equals(URI.create("urn:topic:1")));
        assertAnyTrue(updated.get().getNodeResources(),
                nodeResource -> nodeResource.getNode().get().getPublicId().equals(URI.create("urn:topic:2")));
        assertAnyTrue(updated.get().getAllPaths(), path -> path.equals("/subject:1/topic:1/resource:1"));
        assertAnyTrue(updated.get().getAllPaths(), path -> path.equals("/subject:1/topic:2/resource:1"));
    }

    @Test
    @Transactional
    void can_publish_node_in_tree_to_schema() {
        final var command = new VersionCommand() {
            {
                name = "Beta";
            }
        };
        Version target = versionService.createNewVersion(Optional.empty(), command);
        assertTrue(checkSchemaExists(versionService.schemaFromHash(target.getHash())));

        Node child = builder.node(n -> n.publicId("urn:node:1").isContext(true).isVisible(false)
                .resource(r2 -> r2.publicId("urn:resource:2")));
        Node node = builder.node(NodeType.SUBJECT,
                s -> s.isContext(true).publicId("urn:subject:1").child(NodeType.TOPIC, t -> t.publicId("urn:topic:1"))
                        .child(child)
                        .child(NodeType.TOPIC, t3 -> t3.publicId("urn:topic:3").grepCode("TT2").isVisible(false)));

        TestTransaction.flagForCommit();
        TestTransaction.end();

        nodeService.publishNode(node.getPublicId(), Optional.empty(), target.getPublicId());

        VersionContext.setCurrentVersion(versionService.schemaFromHash(target.getHash()));
        Optional<Node> updatedChild = nodeRepository.fetchNodeGraphByPublicId(child.getPublicId());
        VersionContext.setCurrentVersion(versionService.schemaFromHash(null));

        assertTrue(updatedChild.isPresent());
        assertFalse(updatedChild.get().getMetadata().isVisible());
        assertFalse(updatedChild.get().getNodeResources().isEmpty());
        assertTrue(updatedChild.get().getParentNode().isPresent());
        assertEquals(node.getPublicId(), updatedChild.get().getParentNode().get().getPublicId());
        assertAnyTrue(updatedChild.get().getAllPaths(), path -> path.equals("/subject:1/node:1"));
    }
}
