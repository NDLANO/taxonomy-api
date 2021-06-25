package no.ndla.taxonomy.rest.v1;

import no.ndla.taxonomy.domain.Status;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;

import static no.ndla.taxonomy.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class StatusesTest extends RestTest {

    @Test
    public void can_get_all_statuses() throws Exception {
        builder.status(s -> s.name("draft"));
        builder.status(s -> s.name("new"));

        MockHttpServletResponse response = testUtils.getResource("/v1/statuses");
        Statuses.StatusIndexDocument[] statuses = testUtils.getObject(Statuses.StatusIndexDocument[].class, response);

        assertEquals(2, statuses.length);
        assertAnyTrue(statuses, s -> "draft".equals(s.name));
        assertAnyTrue(statuses, s -> "new".equals(s.name));
        assertAllTrue(statuses, s -> isValidId(s.id));
    }

    @Test
    public void can_get_status_by_id() throws Exception {
        URI id = builder.status(s -> s.name("draft")).getPublicId();

        MockHttpServletResponse response = testUtils.getResource("/v1/statuses/" + id.toString());
        Statuses.StatusIndexDocument status = testUtils.getObject(Statuses.StatusIndexDocument.class, response);
        assertEquals(id, status.id);
    }

    @Test
    public void can_get_all_statuses_with_translation() throws Exception {
        builder.status(s -> s.name("draft").translation("nb", tr -> tr.name("utkast")));
        builder.status(s -> s.name("new").translation("nb", tr -> tr.name("ny")));

        MockHttpServletResponse response = testUtils.getResource("/v1/statuses?language=nb");
        Statuses.StatusIndexDocument[] statuses = testUtils.getObject(Statuses.StatusIndexDocument[].class, response);

        assertEquals(2, statuses.length);
        assertAnyTrue(statuses, s -> "utkast".equals(s.name));
        assertAnyTrue(statuses, s -> "ny".equals(s.name));
    }

    @Test
    public void can_get_status_by_id_with_translation() throws Exception {
        URI id = builder.status(s -> s.name("draft").translation("nb", tr -> tr.name("utkast"))).getPublicId();

        MockHttpServletResponse response = testUtils.getResource("/v1/statuses/" + id.toString() + "?language=nb");
        Statuses.StatusIndexDocument status = testUtils.getObject(Statuses.StatusIndexDocument.class, response);
        assertEquals("utkast", status.name);
    }

    @Test
    public void unknown_status_fails_gracefully() throws Exception {
        testUtils.getResource("/v1/statuses/doesnotexist", status().isNotFound());
    }

    @Test
    public void can_create_status() throws Exception {
        Statuses.StatusCommand command = new Statuses.StatusCommand() {{
            id = URI.create("urn:status:1");
            name = "name";
        }};

        testUtils.createResource("/v1/statuses/", command);

        Status result = statusRepository.getByPublicId(command.id);
        assertEquals(command.name, result.getName());
    }

    @Test
    public void cannot_create_duplicate_status() throws Exception {
        Statuses.StatusCommand command = new Statuses.StatusCommand() {{
            id = URI.create("urn:status:1");
            name = "name";
        }};
        testUtils.createResource("/v1/statuses/", command);
        testUtils.createResource("/v1/statuses/", command, status().isConflict());
    }

    @Test
    public void can_delete_status() throws Exception {
        URI id = builder.status(s -> s.name("draft")).getPublicId();
        testUtils.deleteResource("/v1/statuses/" + id);
        assertNull(statusRepository.findByPublicId(id));
    }

    @Test
    public void can_update_status() throws Exception {
        URI id = builder.status(s -> s.name("daft")).getPublicId();

        testUtils.updateResource("/v1/statuses/" + id, new Statuses.StatusCommand() {{
            name = "draft";
        }});

        Status result = statusRepository.getByPublicId(id);
        assertEquals("draft", result.getName());
    }

    @Test
    public void can_change_status_id() throws Exception {
        URI id = builder.status(s -> s.name("draft")).getPublicId();

        testUtils.updateResource("/v1/statuses/" + id, new Statuses.StatusCommand() {{
            name = "draft";
            id = URI.create("urn:status:draft");
        }});

        Status result = statusRepository.getByPublicId(URI.create("urn:status:draft"));
        assertEquals("urn:status:draft", result.getPublicId().toString());
    }
}

