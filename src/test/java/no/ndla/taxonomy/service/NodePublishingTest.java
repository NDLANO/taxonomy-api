/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Builder;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.domain.Version;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.repositories.VersionRepository;
import no.ndla.taxonomy.rest.v1.commands.VersionCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

@SpringBootTest
@Transactional
public class NodePublishingTest extends AbstractIntegrationTest {

    @Autowired
    VersionRepository versionRepository;

    @Autowired
    NodeRepository nodeRepository;

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
        Node node = builder.node();

        TestTransaction.flagForCommit();
        TestTransaction.end();

        nodeService.publishNode(node.getPublicId(), Optional.empty(), target.getPublicId());
    }

    //@Test
    void can_publish_node_between_schemas() {
        final var command = new VersionCommand() {
            {
                name = "Beta";
            }
        };
        Version source = versionService.createNewVersion(Optional.empty(), command);
        assertTrue(checkSchemaExists(versionService.schemaFromHash(source.getHash())));
        Version target = versionService.createNewVersion(Optional.empty(), command);
        assertTrue(checkSchemaExists(versionService.schemaFromHash(target.getHash())));

        //VersionContext.setCurrentVersion(versionService.schemaFromHash(source.getHash()));
        Node node = builder.node();

        TestTransaction.flagForCommit();
        TestTransaction.end();

        nodeService.publishNode(node.getPublicId(), Optional.empty(), target.getPublicId());
    }
}
