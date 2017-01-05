package no.ndla.taxonomy.service.rest;


import no.ndla.taxonomy.service.GraphFactory;
import no.ndla.taxonomy.service.domain.Resource;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.URI;

import static no.ndla.taxonomy.service.TestUtils.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("junit")
public class ResourcesTest {

    @Autowired
    private GraphFactory factory;

    @Before
    public void setup() throws Exception {
        clearGraph();
    }

    @Test
    public void can_get_all_resources() throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            new Resource(graph).name("The inner planets");
            new Resource(graph).name("Gas giants");
            transaction.commit();
        }

        MockHttpServletResponse response = getResource("/resources");
        Resources.ResourceIndexDocument[] resources = getObject(Resources.ResourceIndexDocument[].class, response);
        assertEquals(2, resources.length);

        assertAnyTrue(resources, s -> "The inner planets".equals(s.name));
        assertAnyTrue(resources, s -> "Gas giants".equals(s.name));
        assertAllTrue(resources, s -> isValidId(s.id));
    }

    @Test
    public void can_create_resource() throws Exception {
        Resources.CreateResourceCommand createResourceCommand = new Resources.CreateResourceCommand();
        createResourceCommand.name = "testresource";

        MockHttpServletResponse response = createResource("/resources", createResourceCommand);
        String id = getId(response);

        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            Resource resource = Resource.getById(id, graph);
            assertEquals(createResourceCommand.name, resource.getName());
            transaction.rollback();
        }
    }


    @Test
    public void can_update_resource() throws Exception {
        String id;
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            id = new Resource(graph).getId().toString();
            transaction.commit();
        }

        Resources.UpdateResourceCommand command = new Resources.UpdateResourceCommand();
        command.name = "The inner planets";

        updateResource("/resources/" + id, command);

        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            Resource resource = Resource.getById(id, graph);
            assertEquals(command.name, resource.getName());
            transaction.rollback();
        }
    }

    @Test
    public void can_create_resource_with_id() throws Exception {
        Resources.CreateResourceCommand command = new Resources.CreateResourceCommand() {{
            id = URI.create("urn:resource:1");
            name = "name";
        }};

        createResource("/resources", command);

        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            assertNotNull(Resource.getById(command.id.toString(), graph));
            transaction.rollback();
        }
    }

    @Test
    public void duplicate_ids_not_allowed() throws Exception {
        Resources.CreateResourceCommand command = new Resources.CreateResourceCommand() {{
            id = URI.create("urn:resource:1");
            name = "name";
        }};

        createResource("/resources", command, status().isCreated());
        createResource("/resources", command, status().isConflict());
    }

    @Test
    public void can_delete_resource() throws Exception {
        String id;
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            id = new Resource(graph).getId().toString();
            transaction.commit();
        }

        deleteResource("/resources/" + id);
        assertNotFound(graph -> Resource.getById(id, graph));
    }

    @Test
    public void can_get_resource_by_id() throws Exception {
        String id;
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            id = new Resource(graph).name("The inner planets").getId().toString();
            transaction.commit();
        }
        MockHttpServletResponse response = getResource("/resources/" + id);
        Resources.ResourceIndexDocument result = getObject(Resources.ResourceIndexDocument.class, response);
        assertEquals(id, result.id.toString());
    }

    @Test
    public void get_unknown_resource_fails_gracefully() throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            new Resource(graph).name("The inner planets").getId().toString();
            transaction.commit();
        }

        getResource("/resources/nonexistantid", status().isNotFound());

    }
}
