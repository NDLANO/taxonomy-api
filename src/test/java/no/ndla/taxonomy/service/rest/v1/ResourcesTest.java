package no.ndla.taxonomy.service.rest.v1;


import no.ndla.taxonomy.service.domain.Resource;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;

import static no.ndla.taxonomy.service.TestUtils.*;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ResourcesTest extends RestTest {

    @Test
    public void can_get_single_resource() throws Exception {
        URI trigonometry = builder.resource(s -> s
                .name("introduction to trigonometry")
                .contentUri("urn:article:1")
        ).getPublicId();

        MockHttpServletResponse response = getResource("/v1/resources/" + trigonometry);
        Resources.ResourceIndexDocument resource = getObject(Resources.ResourceIndexDocument.class, response);

        assertEquals("introduction to trigonometry", resource.name);
        assertEquals("urn:article:1", resource.contentUri.toString());
    }

    @Test
    public void can_get_all_resources() throws Exception {
        newResource().name("The inner planets");
        newResource().name("Gas giants");

        MockHttpServletResponse response = getResource("/v1/resources");
        Resources.ResourceIndexDocument[] resources = getObject(Resources.ResourceIndexDocument[].class, response);
        assertEquals(2, resources.length);

        assertAnyTrue(resources, s -> "The inner planets".equals(s.name));
        assertAnyTrue(resources, s -> "Gas giants".equals(s.name));
        assertAllTrue(resources, s -> isValidId(s.id));
    }

    @Test
    public void can_create_resource() throws Exception {
        Resources.CreateResourceCommand createResourceCommand = new Resources.CreateResourceCommand() {{
            name = "testresource";
            contentUri = URI.create("urn:article:1");
        }};

        URI id = getId(createResource("/v1/resources", createResourceCommand));

        Resource resource = resourceRepository.getByPublicId(id);
        assertEquals(createResourceCommand.name, resource.getName());
        assertEquals(createResourceCommand.contentUri, resource.getContentUri());
    }

    @Test
    public void can_update_resource() throws Exception {
        URI id = newResource().getPublicId();

        Resources.UpdateResourceCommand command = new Resources.UpdateResourceCommand() {{
            name = "The inner planets";
            contentUri = URI.create("urn:article:1");
        }};

        updateResource("/v1/resources/" + id, command);

        Resource resource = resourceRepository.getByPublicId(id);
        assertEquals(command.name, resource.getName());
        assertEquals(command.contentUri, resource.getContentUri());
    }

    @Test
    public void can_create_resource_with_id() throws Exception {
        Resources.CreateResourceCommand command = new Resources.CreateResourceCommand() {{
            id = URI.create("urn:resource:1");
            name = "name";
        }};

        createResource("/v1/resources", command);

        assertNotNull(resourceRepository.getByPublicId(command.id));
    }

    @Test
    public void duplicate_ids_not_allowed() throws Exception {
        Resources.CreateResourceCommand command = new Resources.CreateResourceCommand() {{
            id = URI.create("urn:resource:1");
            name = "name";
        }};

        createResource("/v1/resources", command, status().isCreated());
        createResource("/v1/resources", command, status().isConflict());
    }

    @Test
    public void can_delete_resource() throws Exception {
        URI id = newResource().getPublicId();
        deleteResource("/v1/resources/" + id);
        assertNull(resourceRepository.findByPublicId(id));
    }

    @Test
    public void can_get_resource_by_id() throws Exception {
        URI id = newResource().name("The inner planets").getPublicId();

        MockHttpServletResponse response = getResource("/v1/resources/" + id);
        Resources.ResourceIndexDocument result = getObject(Resources.ResourceIndexDocument.class, response);

        assertEquals(id, result.id);
    }

    @Test
    public void get_unknown_resource_fails_gracefully() throws Exception {
        getResource("/v1/resources/nonexistantid", status().isNotFound());
    }
}
