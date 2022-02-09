/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class MetadataServiceImplTest {
    private DomainEntityHelperService domainEntityHelperService;
    private GrepCodeService grepCodeService;
    private CustomFieldService customFieldService;
    private MetadataServiceImpl metadataService;

    @BeforeEach
    void setUp(@Autowired CustomFieldService customFieldService,
            @Autowired DomainEntityHelperService domainEntityHelperService,
            @Autowired GrepCodeService grepCodeService) {
        this.domainEntityHelperService = domainEntityHelperService;
        this.grepCodeService = grepCodeService;
        this.customFieldService = customFieldService;
        metadataService = new MetadataServiceImpl(domainEntityHelperService, grepCodeService, customFieldService);
    }

    @Test
    @Transactional
    void get_metadata_for_nonexistent_uri() {
        assertThrows(NotFoundServiceException.class,
                () -> domainEntityHelperService.getEntityByPublicId(URI.create("urn:topic:test:3")));
    }

    /*
     * @Test
     * 
     * @Transactional void getMetadata() { assertFalse(metadataService.getMetadata("urn:test:1300").isPresent());
     * 
     * final var taxonomyEntity = new Metadata(); taxonomyEntity.setPublicId("urn:test:1300");
     * metadataRepository.saveAndFlush(taxonomyEntity);
     * 
     * assertTrue(metadataService.getMetadata("urn:test:1300").isPresent()); assertSame(taxonomyEntity,
     * metadataService.getMetadata("urn:test:1300").orElseThrow()); }
     * 
     * @Test
     * 
     * @Transactional void getOrCreateMetadata() {
     * assertFalse(metadataService.getMetadata("urn:test:1301").isPresent());
     * 
     * final var metadata1 = metadataService.getOrCreateMetadata("urn:test:1301");
     * 
     * assertNotNull(metadata1); assertNotNull(metadata1.getId()); assertEquals("urn:test:1301",
     * metadata1.getPublicId());
     * 
     * assertTrue(metadataService.getMetadata("urn:test:1301").isPresent()); assertSame(metadata1,
     * metadataService.getMetadata("urn:test:1301").orElseThrow());
     * 
     * assertFalse(metadataService.getMetadata("urn:test:1302").isPresent());
     * 
     * final var metadata2 = new Metadata(); metadata2.setPublicId("urn:test:1302");
     * metadataRepository.saveAndFlush(metadata2);
     * 
     * assertTrue(metadataService.getMetadata("urn:test:1302").isPresent()); assertSame(metadata2,
     * metadataService.getMetadata("urn:test:1302").orElseThrow()); }
     * 
     * @Test void saveMetadata() { final var metadata = new Metadata(); metadata.setPublicId("urn:test:1303");
     * 
     * assertNull(metadata.getId());
     * 
     * assertFalse(metadataRepository.findFirstByPublicId("urn:test:1303").isPresent());
     * 
     * metadataService.saveMetadata(metadata);
     * 
     * assertNotNull(metadata.getId());
     * 
     * assertTrue(metadataRepository.findFirstByPublicId("urn:test:1303").isPresent()); }
     * 
     * @Test
     * 
     * @Transactional void deleteMetadata(@Autowired GrepCodeRepository grepCodeRepository) { final var metadata1 = new
     * Metadata(); metadata1.setPublicId("urn:test:34"); metadataRepository.saveAndFlush(metadata1);
     * 
     * final var metadata2 = new Metadata(); metadata2.setPublicId("urn:test:35");
     * metadataRepository.saveAndFlush(metadata2);
     * 
     * final var grepCode1 = new GrepCode(); grepCode1.setCode("A1"); final var grepCode2 = new GrepCode();
     * grepCode2.setCode("A2");
     * 
     * metadata1.addGrepCode(grepCode1); metadata1.addGrepCode(grepCode2); metadata2.addGrepCode(grepCode2);
     * 
     * grepCodeRepository.saveAll(Set.of(grepCode1, grepCode2));
     * 
     * assertNotNull(grepCode1.getId()); assertNotNull(grepCode2.getId());
     * 
     * final var code1Id = grepCode1.getId(); final var code2Id = grepCode2.getId(); final var entityId1 =
     * metadata1.getId(); final var entityId2 = metadata2.getId();
     * 
     * assertTrue(grepCodeRepository.findById(code1Id).isPresent());
     * assertTrue(grepCodeRepository.findById(code2Id).isPresent());
     * assertTrue(metadataRepository.findById(entityId1).isPresent());
     * 
     * assertEquals(1, grepCode1.getMetadata().size()); assertEquals(2, grepCode2.getMetadata().size());
     * assertTrue(grepCode1.getMetadata().contains(metadata1));
     * assertTrue(grepCode2.getMetadata().containsAll(Set.of(metadata1, metadata2)));
     * 
     * metadataService.deleteMetadata("urn:test:34");
     * 
     * assertTrue(grepCodeRepository.findById(code2Id).isPresent());
     * 
     * assertFalse(metadataRepository.findById(entityId1).isPresent());
     * assertTrue(metadataRepository.findById(entityId2).isPresent());
     * 
     * assertEquals(0, grepCode1.getMetadata().size()); assertEquals(1, grepCode2.getMetadata().size());
     * assertTrue(grepCode2.getMetadata().contains(metadata2));
     * 
     * metadataService.deleteMetadata("urn:test:35");
     * 
     * assertEquals(0, grepCode1.getMetadata().size()); assertEquals(0, grepCode2.getMetadata().size());
     * 
     * assertFalse(metadataRepository.findById(entityId2).isPresent()); }
     * 
     * @Test
     * 
     * @Transactional void getMetadataList() { final var metadata1 = new Metadata();
     * metadata1.setPublicId("urn:test:1"); metadata1.setVisible(true);
     * 
     * final var metadata2 = new Metadata(); metadata2.setPublicId("urn:test:2"); metadata2.setVisible(false);
     * 
     * final var metadata3 = new Metadata(); metadata3.setPublicId("urn:test:3"); metadata3.setVisible(true);
     * 
     * metadataRepository.saveAll(Set.of(metadata1, metadata2, metadata3));
     * 
     * final var returned1 = metadataService.getMetadataList(Set.of("urn:test:1", "urn:test:3")); assertEquals(2,
     * returned1.size()); assertTrue(returned1.containsAll(Set.of(metadata1, metadata3)));
     * 
     * final var returned2 = metadataService.getMetadataList(Set.of()); assertEquals(0, returned2.size());
     * 
     * final var returned3 = metadataService.getMetadataList(Set.of("urn:test:1", "urn:test:4")); assertEquals(1,
     * returned3.size()); assertTrue(returned3.contains(metadata1)); }
     * 
     * @Test
     * 
     * @Transactional void getOrCreateMetadataList() { final var existingMetadataToCreate = new Metadata();
     * existingMetadataToCreate.setPublicId("urn:test:2002"); existingMetadataToCreate.setVisible(false); final var
     * existingEntity = metadataRepository.saveAndFlush(existingMetadataToCreate);
     * 
     * // Should create urn:test:2001, but return existing urn:test:2002 final var metadataList =
     * metadataService.getOrCreateMetadataList(List.of("urn:test:2001", "urn:test:2002"));
     * 
     * // Returned list is ordered the same order as received (when ordered) final var returnedMetadata1 =
     * metadataList.get(0); final var returnedMetadata2 = metadataList.get(1);
     * 
     * assertSame(returnedMetadata2, existingEntity); assertEquals("urn:test:2002", returnedMetadata2.getPublicId());
     * assertEquals(0, returnedMetadata2.getGrepCodes().size()); assertFalse(returnedMetadata2.isVisible());
     * 
     * assertEquals("urn:test:2001", returnedMetadata1.getPublicId()); assertEquals(0,
     * returnedMetadata1.getGrepCodes().size()); assertTrue(returnedMetadata1.isVisible());
     * assertSame(returnedMetadata1, metadataRepository.findFirstByPublicId("urn:test:2001").orElseThrow()); }
     * 
     * @Test void saveMetadataList() { final var metadata1 = new Metadata(); metadata1.setPublicId("urn:test:2103");
     * final var metadata2 = new Metadata(); metadata2.setPublicId("urn:test:2104");
     * 
     * assertNull(metadata1.getId()); assertNull(metadata2.getId());
     * 
     * assertFalse(metadataRepository.findFirstByPublicId("urn:test:2103").isPresent());
     * assertFalse(metadataRepository.findFirstByPublicId("urn:test:2104").isPresent());
     * 
     * metadataService.saveMetadataList(Set.of(metadata1, metadata2));
     * 
     * assertNotNull(metadata1.getId()); assertNotNull(metadata2.getId());
     * 
     * assertTrue(metadataRepository.findFirstByPublicId("urn:test:2103").isPresent());
     * assertTrue(metadataRepository.findFirstByPublicId("urn:test:2104").isPresent()); }
     */
}
