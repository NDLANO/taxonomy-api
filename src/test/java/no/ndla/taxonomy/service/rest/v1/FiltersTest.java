package no.ndla.taxonomy.service.rest.v1;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static no.ndla.taxonomy.service.TestUtils.getObject;
import static no.ndla.taxonomy.service.TestUtils.getResource;
import static org.junit.Assert.assertEquals;

public class FiltersTest extends RestTest {

    @Test
    public void can_get_a_single_filter() throws Exception {
        builder.filter(f -> f
                .publicId("urn:filter:1")
                .name("1T-YF")
        );

        MockHttpServletResponse response = getResource("/v1/filters/" + "urn:filter:1");
        Filters.FilterIndexDocument filter = getObject(Filters.FilterIndexDocument.class, response);

        assertEquals("1T-YF", filter.name);
    }
}
