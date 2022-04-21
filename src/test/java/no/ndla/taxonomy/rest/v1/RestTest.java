/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import no.ndla.taxonomy.TestUtils;
import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.repositories.*;
import no.ndla.taxonomy.service.CachedUrlUpdaterService;
import no.ndla.taxonomy.service.dtos.MetadataDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.net.URI;
import java.util.Set;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("junit")
@Transactional
public abstract class RestTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    VersionRepository versionRepository;

    @Autowired
    ResourceResourceTypeRepository resourceResourceTypeRepository;

    @Autowired
    ResourceRepository resourceRepository;

    @Autowired
    ResourceTypeRepository resourceTypeRepository;

    @Autowired
    RelevanceRepository relevanceRepository;

    @Autowired
    NodeRepository nodeRepository;

    @Autowired
    NodeConnectionRepository nodeConnectionRepository;

    @Autowired
    NodeResourceRepository nodeResourceRepository;

    @Autowired
    protected TestUtils testUtils;

    @Autowired
    protected CachedUrlUpdaterService cachedUrlUpdaterService;

    protected Builder builder;

    private MetadataDto createMetadataObject(URI publicId) {
        final var metadata = new MetadataDto();
        metadata.setPublicId(publicId.toString());

        // Can search for RESOURCE1 where publicId is urn:resource:1 in the test
        metadata.setGrepCodes(Set.of(publicId.getSchemeSpecificPart().replace(":", "").toUpperCase()));

        metadata.setVisible(true);

        return metadata;
    }

    @SuppressWarnings("unchecked")
    @BeforeEach
    public void restTestSetUp() {
        builder = new Builder(entityManager, cachedUrlUpdaterService);
        resourceRepository.deleteAllAndFlush();
        nodeRepository.deleteAllAndFlush();
        nodeResourceRepository.deleteAllAndFlush();
        nodeConnectionRepository.deleteAllAndFlush();
    }

    <T> T save(T entity) {
        entityManager.persist(entity);
        return entity;
    }

    Node newSubject() {
        Node node = new Node(NodeType.SUBJECT);
        node.setContext(true);
        return save(node);
    }

    Node newTopic() {
        Node node = new Node(NodeType.TOPIC);
        return save(node);
    }

    Resource newResource() {
        return save(new Resource());
    }

    ResourceType newResourceType() {
        return save(new ResourceType());
    }
}
