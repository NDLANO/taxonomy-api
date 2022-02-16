/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Builder;
import no.ndla.taxonomy.domain.NodeType;
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

    @BeforeEach
    void setUp(@Autowired CustomFieldService customFieldService,
            @Autowired DomainEntityHelperService domainEntityHelperService, @Autowired GrepCodeService grepCodeService,
            @Autowired Builder builder) {
        this.domainEntityHelperService = domainEntityHelperService;
        this.grepCodeService = grepCodeService;
        this.customFieldService = customFieldService;
        this.builder = builder;
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

        MetadataDto updatedMetadata = metadataService.updateMetadataByPublicId(publicId, metadata);
        assertFalse(updatedMetadata.isVisible());
        assertTrue(updatedMetadata.getGrepCodes().contains("KM123"));
        assertTrue(updatedMetadata.getCustomFields().containsKey("Gunnar"));
        assertTrue(updatedMetadata.getCustomFields().containsValue("ruler"));
    }

}
