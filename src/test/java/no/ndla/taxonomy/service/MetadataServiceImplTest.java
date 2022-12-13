/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Builder;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeConnection;
import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.repositories.NodeConnectionRepository;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.service.dtos.MetadataDto;
import no.ndla.taxonomy.service.exceptions.InvalidDataException;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class MetadataServiceImplTest {
    private DomainEntityHelperService domainEntityHelperService;
    private GrepCodeService grepCodeService;
    private CustomFieldService customFieldService;
    private MetadataServiceImpl metadataService;
    private Builder builder;
    private NodeRepository nodeRepository;
    private NodeConnectionRepository nodeConnectionRepository;

    @BeforeEach
    void setUp(@Autowired CustomFieldService customFieldService,
            @Autowired DomainEntityHelperService domainEntityHelperService, @Autowired GrepCodeService grepCodeService,
            @Autowired Builder builder, @Autowired NodeConnectionRepository nodeConnectionRepository,
            @Autowired NodeRepository nodeRepository) {
        this.domainEntityHelperService = domainEntityHelperService;
        this.grepCodeService = grepCodeService;
        this.customFieldService = customFieldService;
        this.builder = builder;
        this.nodeRepository = nodeRepository;
        this.nodeConnectionRepository = nodeConnectionRepository;
        metadataService = new MetadataServiceImpl(domainEntityHelperService, grepCodeService, customFieldService);
    }

    @Test
    @Transactional
    void get_metadata_for_nonexistent_uri() {
        assertThrows(NotFoundServiceException.class,
                () -> domainEntityHelperService.getEntityByPublicId(URI.create("urn:topic:test:3")));
    }

    @Test
    @Transactional
    void get_metadata_for_node() {
        URI publicId = builder
                .node(NodeType.SUBJECT, s -> s.isVisible(false).grepCode("GREP1").customField("key", "value"))
                .getPublicId();
        MetadataDto metadata = metadataService.getMetadataByPublicId(publicId);

        assertFalse(metadata.isVisible());
        assertTrue(metadata.getGrepCodes().contains("GREP1"));
        assertTrue(metadata.getCustomFields().containsKey("key"));
        assertTrue(metadata.getCustomFields().containsValue("value"));
    }

    @Test
    @Transactional
    void can_update_metadata_for_node() throws InvalidDataException {
        URI publicId = builder.node().getPublicId();

        MetadataDto metadata = new MetadataDto();
        metadata.setVisible(false);
        metadata.setGrepCodes(Set.of("KM123"));
        metadata.setCustomFields(Map.of("Gunnar", "ruler"));

        metadataService.updateMetadataByPublicId(publicId, metadata);
        Node node = nodeRepository.getByPublicId(publicId);

        MetadataDto metadataDto = new MetadataDto(node.getMetadata());
        assertFalse(metadataDto.isVisible());
        assertTrue(metadataDto.getGrepCodes().contains("KM123"));
        assertTrue(metadataDto.getCustomFields().containsKey("Gunnar"));
        assertTrue(metadataDto.getCustomFields().containsValue("ruler"));
    }

    @Test
    @Transactional
    void update_metadata_for_nodeConnection_updates_child() throws InvalidDataException {
        Node child = builder.node();
        Node parent = builder.node(n -> n.child(child));
        NodeConnection connection = parent.getChildren().stream().findFirst().get();

        MetadataDto metadata = new MetadataDto();
        metadata.setVisible(false);
        metadata.setGrepCodes(Set.of("KM123"));
        metadata.setCustomFields(Map.of("Gunnar", "ruler"));

        metadataService.updateMetadataByPublicId(connection.getPublicId(), metadata);
        NodeConnection updated = nodeConnectionRepository.findByPublicId(connection.getPublicId());
        MetadataDto connectionMetadata = new MetadataDto(updated.getMetadata());
        assertFalse(connectionMetadata.isVisible());
        assertTrue(connectionMetadata.getGrepCodes().contains("KM123"));
        assertTrue(connectionMetadata.getCustomFields().containsKey("Gunnar"));
        assertTrue(connectionMetadata.getCustomFields().containsValue("ruler"));

        Node node = nodeRepository.getByPublicId(child.getPublicId());
        MetadataDto childMetadata = new MetadataDto(node.getMetadata());
        assertFalse(childMetadata.isVisible());
        assertTrue(childMetadata.getGrepCodes().contains("KM123"));
        assertTrue(childMetadata.getCustomFields().containsKey("Gunnar"));
        assertTrue(childMetadata.getCustomFields().containsValue("ruler"));
    }

    @Test
    @Transactional
    void update_metadata_for_nodeConnection_does_not_update_child_if_value_is_set() throws InvalidDataException {
        Node child = builder.node();
        Node parent = builder.node(n -> n.child(child));
        NodeConnection connection = parent.getChildren().stream().findFirst().get();

        MetadataDto metadata = new MetadataDto();
        metadata.setVisible(false);
        metadata.setGrepCodes(Set.of("KM123"));
        metadata.setCustomFields(Map.of("Gunnar", "ruler"));

        // Turn off updating children
        metadataService.setUpdateChildRelation(false);

        metadataService.updateMetadataByPublicId(connection.getPublicId(), metadata);
        NodeConnection updated = nodeConnectionRepository.findByPublicId(connection.getPublicId());
        MetadataDto connectionMetadata = new MetadataDto(updated.getMetadata());
        assertFalse(connectionMetadata.isVisible());
        assertTrue(connectionMetadata.getGrepCodes().contains("KM123"));
        assertTrue(connectionMetadata.getCustomFields().containsKey("Gunnar"));
        assertTrue(connectionMetadata.getCustomFields().containsValue("ruler"));

        Node node = nodeRepository.getByPublicId(child.getPublicId());
        MetadataDto childMetadata = new MetadataDto(node.getMetadata());
        assertTrue(childMetadata.isVisible());
        assertFalse(childMetadata.getGrepCodes().contains("KM123"));
        assertFalse(childMetadata.getCustomFields().containsKey("Gunnar"));
        assertFalse(childMetadata.getCustomFields().containsValue("ruler"));
    }

}
