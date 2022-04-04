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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Must be run separately in IDE, excluded from mvn build because of shared database state.
 */
@SpringBootTest
@Transactional
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
    NodeService nodeService;

    @Autowired
    VersionService versionService;

    @Autowired
    Builder builder;

    @BeforeEach
    void clearAllRepos() {
        versionRepository.deleteAllAndFlush();
        nodeRepository.deleteAllAndFlush();
        nodeConnectionRepository.deleteAllAndFlush();
        resourceRepository.deleteAllAndFlush();
        nodeResourceRepository.deleteAllAndFlush();
    }

    @Test
    void can_publish_node_to_schema() {
        final var command = new VersionCommand() {
            {
                name = "Beta";
            }
        };
        Version target = versionService.createNewVersion(Optional.empty(), command);
        assertTrue(checkSchemaExists(versionService.schemaFromHash(target.getHash())));

        VersionContext.setCurrentVersion(versionService.schemaFromHash(null));
        Node node = builder.node(n -> n.publicId("urn:node:1").grepCode("KM123").customField("key", "value"));

        TestTransaction.flagForCommit();
        TestTransaction.end();

        nodeService.publishNode(node.getPublicId(), Optional.empty(), target.getPublicId());
    }

    @Test
    void can_publish_node_with_resource_to_schema() {
        final var command = new VersionCommand() {
            {
                name = "Beta";
            }
        };
        Version target = versionService.createNewVersion(Optional.empty(), command);
        assertTrue(checkSchemaExists(versionService.schemaFromHash(target.getHash())));

        VersionContext.setCurrentVersion(versionService.schemaFromHash(null));
        Node node = builder.node(n -> n.publicId("urn:node:1").grepCode("KM123").customField("key", "value")
                .resource(r -> r.publicId("urn:resource:1").grepCode("KM234").customField("key2", "value2")));

        TestTransaction.flagForCommit();
        TestTransaction.end();

        nodeService.publishNode(node.getPublicId(), Optional.empty(), target.getPublicId());
    }

    @Test
    void can_publish_node_tree_to_schema() {
        final var command = new VersionCommand() {
            {
                name = "Beta";
            }
        };
        Version target = versionService.createNewVersion(Optional.empty(), command);
        assertTrue(checkSchemaExists(versionService.schemaFromHash(target.getHash())));

        VersionContext.setCurrentVersion(versionService.schemaFromHash(null));
        Node node = builder.node(NodeType.SUBJECT,
                s -> s.isContext(true).publicId("urn:subject:1").child(NodeType.TOPIC,
                        t -> t.publicId("urn:topic:1").child(NodeType.TOPIC, st -> st.publicId("urn:topic:2"))));

        TestTransaction.flagForCommit();
        TestTransaction.end();

        nodeService.publishNode(node.getPublicId(), Optional.empty(), target.getPublicId());
    }

    @Test
    void can_publish_sub_node_with_resource_to_schema() {
        final var command = new VersionCommand() {
            {
                name = "Beta";
            }
        };
        Version target = versionService.createNewVersion(Optional.empty(), command);
        assertTrue(checkSchemaExists(versionService.schemaFromHash(target.getHash())));

        VersionContext.setCurrentVersion(versionService.schemaFromHash(null));
        Node node = builder.node(NodeType.SUBJECT, s -> s.isContext(true).publicId("urn:subject:1")
                .child(NodeType.TOPIC, t -> t.publicId("urn:topic:1").resource(r -> r.publicId("urn:resource:1"))));

        TestTransaction.flagForCommit();
        TestTransaction.end();

        nodeService.publishNode(node.getPublicId(), Optional.empty(), target.getPublicId());
    }

    @Test
    void can_publish_node_tree_with_resources_to_schema() {
        final var command = new VersionCommand() {
            {
                name = "Beta";
            }
        };
        Version target = versionService.createNewVersion(Optional.empty(), command);
        assertTrue(checkSchemaExists(versionService.schemaFromHash(target.getHash())));

        VersionContext.setCurrentVersion(versionService.schemaFromHash(null));
        Node node = builder.node(NodeType.SUBJECT, s -> s.isContext(true).publicId("urn:subject:1")
                .child(NodeType.TOPIC, t -> t.publicId("urn:topic:1").resource(r -> r.publicId("urn:resource:1")))
                .child(NodeType.TOPIC, t2 -> t2.publicId("urn:topic:2").resource(r2 -> r2.publicId("urn:resource:2"))
                        .child(NodeType.TOPIC, t3 -> t3.publicId("urn:topic:3").grepCode("TT2").isVisible(false))));

        TestTransaction.flagForCommit();
        TestTransaction.end();

        nodeService.publishNode(node.getPublicId(), Optional.empty(), target.getPublicId());
    }

    @Test
    void can_publish_node_tree_with_reused_resource_to_schema() {
        final var command = new VersionCommand() {
            {
                name = "Beta";
            }
        };
        Version target = versionService.createNewVersion(Optional.empty(), command);
        assertTrue(checkSchemaExists(versionService.schemaFromHash(target.getHash())));

        VersionContext.setCurrentVersion(versionService.schemaFromHash(null));
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
    }

    @Test
    void can_publish_node_in_tree_to_schema() {
        final var command = new VersionCommand() {
            {
                name = "Beta";
            }
        };
        Version target = versionService.createNewVersion(Optional.empty(), command);
        assertTrue(checkSchemaExists(versionService.schemaFromHash(target.getHash())));

        VersionContext.setCurrentVersion(versionService.schemaFromHash(null));
        Node child = builder
                .node(n -> n.publicId("urn:node:1").isVisible(false).resource(r2 -> r2.publicId("urn:resource:2")));
        Node node = builder.node(NodeType.SUBJECT,
                s -> s.isContext(true).publicId("urn:subject:1").child(NodeType.TOPIC, t -> t.publicId("urn:topic:1"))
                        .child(child)
                        .child(NodeType.TOPIC, t3 -> t3.publicId("urn:topic:3").grepCode("TT2").isVisible(false)));

        TestTransaction.flagForCommit();
        TestTransaction.end();

        nodeService.publishNode(node.getPublicId(), Optional.empty(), target.getPublicId());
    }
}
