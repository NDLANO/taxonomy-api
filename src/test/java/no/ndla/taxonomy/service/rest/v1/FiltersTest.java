package no.ndla.taxonomy.service.rest.v1;

import no.ndla.taxonomy.service.domain.Filter;
import no.ndla.taxonomy.service.domain.Subject;
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
    public void can_create_filter() throws Exception {
        builder.subject(s -> s.publicId("urn:subject:1"));

        Filters.CreateFilterCommand command = new Filters.CreateFilterCommand() {{
            id = URI.create("urn:filter:1");
            name = "name";
            subjectId = URI.create("urn:subject:1");
        }};
        createResource("/v1/filters", command, status().isCreated());

        Filter filter = filterRepository.getByPublicId(URI.create("urn:filter:1"));
        assertEquals("urn:subject:1", filter.getSubject().getPublicId().toString());
    }


    @Test
    public void subject_is_required() throws Exception {
        Filters.CreateFilterCommand command = new Filters.CreateFilterCommand() {{
            id = URI.create("urn:filter:1");
            name = "name";
        }};

        createResource("/v1/filters", command, status().isBadRequest());
    }


    @Test
    public void duplicate_ids_not_allowed() throws Exception {
        builder.subject(s -> s.publicId("urn:subject:1"));
        Filters.CreateFilterCommand command = new Filters.CreateFilterCommand() {{
            id = URI.create("urn:filter:1");
            name = "name";
            subjectId = URI.create("urn:subject:1");
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

    @Test
    public void can_add_subject_to_filter() throws Exception {
        builder.subject(s -> s
                .name("Byggfag")
                .publicId("urn:subject:1"));

        builder.filter(f -> f
                .name("Tømrer")
                .publicId("urn:filter:2"));

        updateResource("/v1/filters/urn:filter:2", new Filters.UpdateFilterCommand() {{
            subjectId = URI.create("urn:subject:1");
        }});

        MockHttpServletResponse response = getResource("/v1/filters/urn:filter:2");
        Filters.FilterIndexDocument filter = getObject(Filters.FilterIndexDocument.class, response);

        assertEquals("urn:subject:1", filter.subjectId.toString());
    }

    @Test
    public void can_replace_subject_for_a_filter() throws Exception {
        Subject first = builder.subject(s -> s
                .name("Byggfag")
                .publicId("urn:subject:1"));

        builder.subject(s -> s.name("Ingeniørfag").publicId("urn:subject:3"));

        builder.filter(f -> f
                .name("Tømrer")
                .publicId("urn:filter:2")
                .subject(first));

        updateResource("/v1/filters/urn:filter:2", new Filters.UpdateFilterCommand() {{
            subjectId = URI.create("urn:subject:3");
        }});

        MockHttpServletResponse response = getResource("/v1/filters/urn:filter:2");
        Filters.FilterIndexDocument filter = getObject(Filters.FilterIndexDocument.class, response);

        assertEquals("urn:subject:3", filter.subjectId.toString());
    }

    @Test
    public void can_remove_subject_from_filter() throws Exception {
        Subject subject = builder.subject(s -> s
                .name("Byggfag")
                .publicId("urn:subject:1"));

        builder.filter(f -> f
                .name("Tømrer")
                .publicId("urn:filter:2")
                .subject(subject));

        updateResource("/v1/filters/urn:filter:2", new Filters.UpdateFilterCommand() {{
            subjectId = null;
            name = "Tømrer";
        }});

        MockHttpServletResponse response = getResource("/v1/filters/urn:filter:2");
        Filters.FilterIndexDocument filter = getObject(Filters.FilterIndexDocument.class, response);

        assertEquals(null, filter.subjectId);
    }
}
