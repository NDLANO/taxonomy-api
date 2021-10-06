/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;

import static no.ndla.taxonomy.TestUtils.assertAnyTrue;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class FilterTranslationsTest extends RestTest {


    @Test
    public void can_get_single_filter() throws Exception {
        assert404(new URI("urn:filter:1:1"), "nn");
    }

    private void assert404(URI id, String language) throws Exception {
        String path = "/v1/filters/" + id;
        if (isNotEmpty(language)) path = path + "?language=" + language;
        testUtils.getResource(path, status().isNotFound());
    }

    @Test
    public void can_get_all_translations_for_filters() throws Exception {
        MockHttpServletResponse response = testUtils.getResource("/v1/filters?language=nn");
        final var filters = testUtils.getObject(Object[].class, response);

        assertEquals(0, filters.length);
    }
}