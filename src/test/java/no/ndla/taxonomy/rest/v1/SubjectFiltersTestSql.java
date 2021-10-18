/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import no.ndla.taxonomy.TestSeeder;
import no.ndla.taxonomy.rest.v1.dtos.topics.ResourceIndexDocument;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;

import static no.ndla.taxonomy.TestUtils.assertAnyTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SubjectFiltersTestSql extends RestTest {
    @Autowired private TestSeeder testSeeder;

    @Test
    public void can_get_resources_belonging_to_a_filter_and_resource_type_for_a_subject()
            throws Exception {
        testSeeder.resourceWithFilterAndTypeTestSetup();

        MockHttpServletResponse response =
                testUtils.getResource(
                        "/v1/subjects/urn:subject:1/resources?filter=urn:filter:1&type=urn:resourcetype:video");
        ResourceIndexDocument[] result =
                testUtils.getObject(ResourceIndexDocument[].class, response);

        // Filters are removed
        assertEquals(0, result.length);
    }

    @Test
    public void can_get_resources_belonging_to_a_resource_type_for_a_subject() throws Exception {
        testSeeder.resourceWithFilterAndTypeTestSetup();

        MockHttpServletResponse response =
                testUtils.getResource(
                        "/v1/subjects/urn:subject:1/resources?type=urn:resourcetype:video");
        ResourceIndexDocument[] result =
                testUtils.getObject(ResourceIndexDocument[].class, response);

        // Filters are removed
        assertEquals(1, result.length);
        assertAnyTrue(result, r -> "R:1".equals(r.name));
    }
}
