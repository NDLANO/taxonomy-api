/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.Version;
import no.ndla.taxonomy.domain.VersionType;
import no.ndla.taxonomy.rest.v1.commands.SubjectCommand;
import no.ndla.taxonomy.rest.v1.commands.VersionCommand;
import no.ndla.taxonomy.service.dtos.VersionDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;

import static no.ndla.taxonomy.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

public class VersionsTest extends RestTest {

    @BeforeEach
    void cleanDatabase() {
        versionRepository.deleteAllAndFlush();
    }

    @Test
    public void can_get_all_versions() throws Exception {
        builder.version();
        builder.version();

        MockHttpServletResponse response = testUtils.getResource("/v1/versions");
        VersionDTO[] versions = testUtils.getObject(VersionDTO[].class, response);
        assertEquals(2, versions.length);

    }

    @Test
    public void can_get_specified_version() throws Exception {
        URI versionId = URI.create("urn:version:1");
        builder.version(v -> v.publicId(versionId));

        MockHttpServletResponse response = testUtils.getResource("/v1/versions/" + versionId);
        VersionDTO version = testUtils.getObject(VersionDTO.class, response);
        assertEquals(versionId, version.getId());

        assertNotNull(version.getCreated());
        assertNull(version.getPublished());
        assertNull(version.getArchived());
    }

    @Test
    public void can_get_versions_of_type() throws Exception {
        Version version = builder.version();// BETA
        MockHttpServletResponse response = testUtils.getResource("/v1/versions/?type=BETA");
        VersionDTO[] versions = testUtils.getObject(VersionDTO[].class, response);
        assertEquals(1, versions.length);
        assertAllTrue(versions, v -> v.getVersionType() == VersionType.BETA);
        assertAllTrue(versions, v -> v.getId().equals(version.getPublicId()));

        Version version2 = builder.version(v -> v.type(VersionType.PUBLISHED));
        MockHttpServletResponse response2 = testUtils.getResource("/v1/versions/?type=PUBLISHED");
        VersionDTO[] versions2 = testUtils.getObject(VersionDTO[].class, response2);
        assertEquals(1, versions2.length);
        assertAllTrue(versions2, v -> v.getVersionType() == VersionType.PUBLISHED);
        assertAllTrue(versions2, v -> v.getId().equals(version2.getPublicId()));
    }

    @Test
    public void can_create_version() throws Exception {
        final var createVersionCommand = new VersionCommand() {
            {
                id = URI.create("urn:version:1");
            }
        };

        MockHttpServletResponse response = testUtils.createResource("/v1/versions", createVersionCommand);
        URI id = getId(response);

        Version version = versionRepository.getByPublicId(id);
        assertEquals(createVersionCommand.id, version.getPublicId());
        assertEquals(VersionType.BETA, version.getVersionType());
    }

    @Test
    public void can_update_version() throws Exception {
        Version version = builder.version();// BETA
        URI newUri =  URI.create("urn:version:1");
        final var updateVersionCommand = new VersionCommand() {
            {
                id = newUri;
            }
        };

        MockHttpServletResponse response = testUtils.updateResource("/v1/versions/" + version.getPublicId(), updateVersionCommand);

        Version updated = versionRepository.getByPublicId(newUri);
        assertEquals(updateVersionCommand.id, updated.getPublicId());
        assertEquals(VersionType.BETA, updated.getVersionType());
    }



}
