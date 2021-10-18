/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import no.ndla.taxonomy.domain.Relevance;
import no.ndla.taxonomy.domain.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;

import static no.ndla.taxonomy.TestUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ResourceFiltersTest extends RestTest {

    @Test
    public void can_list_filters_on_resource() throws Exception {
        Relevance relevance = builder.relevance(r -> r.publicId("urn:relevance:core").name("Core material"));

        builder.resource(r -> r.publicId("urn:resource:1"));

        MockHttpServletResponse response = testUtils.getResource("/v1/resources/urn:resource:1/filters");
        final var filters = testUtils.getObject(Object[].class, response);

        assertEquals(0, filters.length);
    }

    @Test
    public void can_list_all_resource_filters() throws Exception {
        Relevance relevance = builder.relevance(r -> r.publicId("urn:relevance:core").name("Core material"));
        builder.resource(r -> r.publicId("urn:resource:1"));

        builder.resource(r -> r.publicId("urn:resource:2"));

        MockHttpServletResponse response = testUtils.getResource("/v1/resource-filters");
        ResourceFilters.ResourceFilterIndexDocument[] resourceFilters = testUtils
                .getObject(ResourceFilters.ResourceFilterIndexDocument[].class, response);
        assertEquals(0, resourceFilters.length);
    }
}
