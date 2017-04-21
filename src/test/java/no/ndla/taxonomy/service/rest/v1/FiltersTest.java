package no.ndla.taxonomy.service.rest.v1;

import no.ndla.taxonomy.service.domain.Filter;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;

import static no.ndla.taxonomy.service.TestUtils.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    @Test
    public void can_get_all_filters() throws Exception {
        builder.filter(f -> f
                .publicId("urn:filter:1")
                .name("1T-YF")
        );

        builder.filter(f -> f
                .publicId("urn:filter:2")
                .name("1T-ST")
        );

        MockHttpServletResponse response = getResource("/v1/filters");
        Filters.FilterIndexDocument[] filters = getObject(Filters.FilterIndexDocument[].class, response);

        assertEquals(2, filters.length);
        assertAnyTrue(filters, f -> f.name.equals("1T-YF"));
        assertAnyTrue(filters, f -> f.name.equals("1T-ST"));
    }

    @Test
    public void can_delete_filter() throws Exception {
        builder.filter(f -> f
                .publicId("urn:filter:1")
                .name("1T-YF")
        );

        deleteResource("/v1/filters/" + "urn:filter:1");
        assertNull(filterRepository.findByPublicId(URI.create("urn:filter:1")));
    }

    @Test
    public void duplicate_ids_not_allowed() throws Exception {
        Filters.CreateFilterCommand command = new Filters.CreateFilterCommand() {{
            id = URI.create("urn:filter:1");
            name = "name";
        }};

        createResource("/v1/filters", command, status().isCreated());
        createResource("/v1/filters", command, status().isConflict());
    }

    @Test
    public void can_update_filter() throws Exception {
        URI id = builder.filter().getPublicId();

        Filters.UpdateFilterCommand command = new Filters.UpdateFilterCommand() {{
            name = "1T-ST";
        }};

        updateResource("/v1/filters/" + id, command);

        Filter filter = filterRepository.getByPublicId(id);
        assertEquals(command.name, filter.getName());
    }

    @Test
    public void get_unknown_filter_fails_gracefully() throws Exception {
        getResource("/v1/filters/nonexistantid", status().isNotFound());
    }
}
