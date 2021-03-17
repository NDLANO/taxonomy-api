package no.ndla.taxonomy.rest.v1;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static no.ndla.taxonomy.TestUtils.assertAnyTrue;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class FiltersTest extends RestTest {

    @Test
    public void can_get_a_single_filter() throws Exception {
        testUtils.getResource("/v1/filters/" + "urn:filter:1", status().isNotFound());
    }

    @Test
    public void can_get_all_filters() throws Exception {
        MockHttpServletResponse response = testUtils.getResource("/v1/filters");
        final var filters = testUtils.getObject(Object[].class, response);

        // Filters are removed
        assertEquals(0, filters.length);
    }

    @Test
    public void get_unknown_filter_fails_gracefully() throws Exception {
        testUtils.getResource("/v1/filters/nonexistantid", status().isNotFound());
    }
}
