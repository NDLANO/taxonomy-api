package no.ndla.taxonomy.rest.v1;

import no.ndla.taxonomy.domain.Filter;
import no.ndla.taxonomy.domain.Subject;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;

import static no.ndla.taxonomy.TestUtils.assertAnyTrue;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class FiltersTest extends RestTest {

    @Test
    public void can_get_a_single_filter() throws Exception {
        builder.filter(f -> f
                .publicId("urn:filter:1")
                .name("1T-YF")
        );

        MockHttpServletResponse response = testUtils.getResource("/v1/filters/" + "urn:filter:1");
        Filters.FilterIndexDocument filter = testUtils.getObject(Filters.FilterIndexDocument.class, response);

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

        MockHttpServletResponse response = testUtils.getResource("/v1/filters");
        Filters.FilterIndexDocument[] filters = testUtils.getObject(Filters.FilterIndexDocument[].class, response);

        assertEquals(2, filters.length);
        assertAnyTrue(filters, f -> f.name.equals("1T-YF"));
        assertAnyTrue(filters, f -> f.name.equals("1T-ST"));
    }

    @Test
    public void can_get_all_filters_with_contentUri() throws Exception {
        builder.filter(f -> f
                .publicId("urn:filter:1")
                .name("F1")
                .contentUri(URI.create("urn:article:1"))
        );

        builder.filter(f -> f
                .publicId("urn:filter:2")
                .name("F2")
                .contentUri(URI.create("urn:article:2"))
        );

        builder.filter(f -> f
                .publicId("urn:filter:3")
                .name("F3")
        );

        MockHttpServletResponse response = testUtils.getResource("/v1/filters");
        Filters.FilterIndexDocument[] filters = testUtils.getObject(Filters.FilterIndexDocument[].class, response);

        assertEquals(3, filters.length);

        for (Filters.FilterIndexDocument filter : filters) {
            switch (filter.name) {
                case "F1":
                    assertEquals(URI.create("urn:article:1"), filter.contentUri);
                    break;
                case "F2":
                    assertEquals(URI.create("urn:article:2"), filter.contentUri);
                    break;
                case "F3":
                    assertNull(filter.contentUri);
                    break;
                default:
                    fail("Unexpected filter returned");
            }
        }
    }

    @Test
    public void can_delete_filter() throws Exception {
        Filter filter = builder.filter(f -> f.publicId("urn:filter:1").name("1T-YF"));

        assertNotNull(filterRepository.findByPublicId(filter.getPublicId()));
        testUtils.deleteResource("/v1/filters/" + filter.getPublicId());
        assertNull(filterRepository.findByPublicId(filter.getPublicId()));
    }

    @Test
    public void can_delete_filter_connected_to_resource() throws Exception {
        final Filter filter = builder.filter(f -> f.publicId("urn:filter:1").name("Vg 1"));

        builder.subject(s -> s
                .publicId("urn:subject:1")
                .topic(t -> t
                        .name("Statics")
                        .publicId("urn:topic:1")
                        .resource(r -> r
                                .publicId("urn:resource:1")
                                .filter(filter, builder.relevance(rel -> rel.publicId("urn:relevance:core"))))));

        Filter preResultFilter = filterRepository.findByPublicId(filter.getPublicId());
        entityManager.refresh(preResultFilter); //as filter.resources would otherwise be empty.

        testUtils.deleteResource("/v1/filters/" + filter.getPublicId());

        Filter resultFilter = filterRepository.findByPublicId(filter.getPublicId());
        assertNull(resultFilter);
    }

    @Test
    public void can_delete_filter_connected_to_2_resources() throws Exception {
        {
            final Filter filter = builder.filter(f -> f.publicId("urn:filter:1").name("Vg 1"));
            builder.subject(s -> s
                    .publicId("urn:subject:1")
                    .topic(t -> t
                            .publicId("urn:topic:1")
                            .resource(r -> r
                                    .publicId("urn:resource:1")
                                    .filter(filter, builder.relevance(rel -> rel.publicId("urn:relevance:core1"))))));
            builder.subject(s -> s
                    .publicId("urn:subject:2")
                    .topic(t -> t
                            .publicId("urn:topic:2")
                            .resource(r -> r
                                    .publicId("urn:resource:2")
                                    .filter(filter, builder.relevance(rel -> rel.publicId("urn:relevance:core2"))))));
        }

        assertNotNull(filterRepository.findByPublicId(new URI("urn:filter:1")));

        testUtils.deleteResource("/v1/filters/urn:filter:1");

        assertNull(filterRepository.findByPublicId(new URI("urn:filter:1")));
    }

    @Test
    public void can_delete_filter_connected_to_subject() throws Exception {
        builder.subject(s -> s.publicId("urn:subject:1"));
        Filters.CreateFilterCommand command = new Filters.CreateFilterCommand() {{
            id = URI.create("urn:filter:1");
            name = "name";
            subjectId = URI.create("urn:subject:1");
        }};
        testUtils.createResource("/v1/filters", command, status().isCreated());
        Filter preResultFilter = filterRepository.findByPublicId(URI.create("urn:filter:1"));
        assertNotNull(preResultFilter);

        testUtils.deleteResource("/v1/filters/" + URI.create("urn:filter:1"));

        Filter resultFilter = filterRepository.findByPublicId(URI.create("urn:filter:1"));
        assertNull(resultFilter);
    }

    @Test
    public void can_delete_filter_connected_to_2_topics() throws Exception {
        builder.subject(s -> s.publicId("urn:subject:1"));
        Filters.CreateFilterCommand command = new Filters.CreateFilterCommand() {{
            id = URI.create("urn:filter:1");
            name = "name";
            subjectId = URI.create("urn:subject:1");
        }};
        testUtils.createResource("/v1/filters", command, status().isCreated());
        Filter filter = filterRepository.findByPublicId(URI.create("urn:filter:1"));
        builder.topic(t -> t.publicId("urn:topic:1").filter(filter, builder.relevance(rel -> rel.publicId("urn:relevance:core1"))));
        builder.topic(t -> t.publicId("urn:topic:2").filter(filter, builder.relevance(rel -> rel.publicId("urn:relevance:core2"))));

        testUtils.deleteResource("/v1/filters/" + URI.create("urn:filter:1"));

        Filter resultFilter = filterRepository.findByPublicId(URI.create("urn:filter:1"));
        assertNull(resultFilter);
    }

    @Test
    public void can_create_filter() throws Exception {
        builder.subject(s -> s.publicId("urn:subject:1"));

        Filters.CreateFilterCommand command = new Filters.CreateFilterCommand() {{
            id = URI.create("urn:filter:1");
            name = "name";
            subjectId = URI.create("urn:subject:1");
        }};
        testUtils.createResource("/v1/filters", command, status().isCreated());

        Filter filter = filterRepository.getByPublicId(URI.create("urn:filter:1"));
        assertEquals("urn:subject:1", filter.getSubject().orElseThrow().getPublicId().toString());
        assertFalse(filter.getContentUri().isPresent());
    }

    @Test
    public void can_create_filter_with_contentUri() throws Exception {
        builder.subject(s -> s.publicId("urn:subject:1"));

        Filters.CreateFilterCommand command = new Filters.CreateFilterCommand() {{
            id = URI.create("urn:filter:1");
            name = "name";
            subjectId = URI.create("urn:subject:1");
            contentUri = URI.create("urn:article:1");
        }};
        testUtils.createResource("/v1/filters", command, status().isCreated());

        Filter filter = filterRepository.getByPublicId(URI.create("urn:filter:1"));
        assertEquals("urn:subject:1", filter.getSubject().orElseThrow().getPublicId().toString());
        assertEquals(URI.create("urn:article:1"), filter.getContentUri().orElseThrow());
    }

    @Test
    public void subject_is_required() throws Exception {
        Filters.CreateFilterCommand command = new Filters.CreateFilterCommand() {{
            id = URI.create("urn:filter:1");
            name = "name";
        }};

        testUtils.createResource("/v1/filters", command, status().isBadRequest());
    }


    @Test
    public void duplicate_ids_not_allowed() throws Exception {
        builder.subject(s -> s.publicId("urn:subject:1"));
        Filters.CreateFilterCommand command = new Filters.CreateFilterCommand() {{
            id = URI.create("urn:filter:1");
            name = "name";
            subjectId = URI.create("urn:subject:1");
        }};

        testUtils.createResource("/v1/filters", command, status().isCreated());
        testUtils.createResource("/v1/filters", command, status().isConflict());
    }

    @Test
    public void can_update_filter() throws Exception {
        builder.subject(s -> s.publicId("urn:subject:1"));

        URI id = builder.filter().getPublicId();

        Filters.UpdateFilterCommand command = new Filters.UpdateFilterCommand() {{
            name = "1T-ST";
            subjectId = URI.create("urn:subject:1");
            contentUri = URI.create("urn:article:2");
        }};

        testUtils.updateResource("/v1/filters/" + id, command);

        Filter filter = filterRepository.getByPublicId(id);
        assertEquals(command.name, filter.getName());
        assertEquals(URI.create("urn:article:2"), filter.getContentUri().orElseThrow());
    }

    @Test
    public void get_unknown_filter_fails_gracefully() throws Exception {
        testUtils.getResource("/v1/filters/nonexistantid", status().isNotFound());
    }

    @Test
    public void can_add_subject_to_filter() throws Exception {
        builder.subject(s -> s
                .name("Byggfag")
                .publicId("urn:subject:1"));

        builder.filter(f -> f
                .name("Tømrer")
                .publicId("urn:filter:2"));

        testUtils.updateResource("/v1/filters/urn:filter:2", new Filters.UpdateFilterCommand() {{
            subjectId = URI.create("urn:subject:1");
        }});

        MockHttpServletResponse response = testUtils.getResource("/v1/filters/urn:filter:2");
        Filters.FilterIndexDocument filter = testUtils.getObject(Filters.FilterIndexDocument.class, response);

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

        testUtils.updateResource("/v1/filters/urn:filter:2", new Filters.UpdateFilterCommand() {{
            subjectId = URI.create("urn:subject:3");
        }});

        MockHttpServletResponse response = testUtils.getResource("/v1/filters/urn:filter:2");
        Filters.FilterIndexDocument filter = testUtils.getObject(Filters.FilterIndexDocument.class, response);

        assertEquals("urn:subject:3", filter.subjectId.toString());
    }
}
