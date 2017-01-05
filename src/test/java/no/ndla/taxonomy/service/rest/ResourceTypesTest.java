package no.ndla.taxonomy.service.rest;

import no.ndla.taxonomy.service.GraphFactory;
import no.ndla.taxonomy.service.domain.Resource;
import no.ndla.taxonomy.service.domain.ResourceType;
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
public class ResourceTypesTest {

    @Autowired
    private GraphFactory factory;

    @Before
    public void setup() throws Exception {
        clearGraph();
    }

    @Test
    public void can_get_all_resource_types() throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            new ResourceType(graph).name("video");
            new ResourceType(graph).name("audio");
            transaction.commit();
        }

        MockHttpServletResponse response = getResource("/resource-types");
        ResourceTypes.ResourceTypeIndexDocument[] resourcetypes = getObject(ResourceTypes.ResourceTypeIndexDocument[].class, response);
        assertEquals(2, resourcetypes.length);

        assertAnyTrue(resourcetypes, s -> "video".equals(s.name));
        assertAnyTrue(resourcetypes, s -> "audio".equals(s.name));
        assertAllTrue(resourcetypes, s -> isValidId(s.id));
    }

    @Test
    public void can_get_resourcetype_by_id() throws Exception {
        URI id;

        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            id = new ResourceType(graph).name("video").getId();
            transaction.commit();
        }


        MockHttpServletResponse response = getResource("/resource-types/" + id.toString());
        ResourceTypes.ResourceTypeIndexDocument resourceType = getObject(ResourceTypes.ResourceTypeIndexDocument.class, response);
        assertEquals(id, resourceType.id);
    }

    @Test
    public void unknown_resourcetype_fails_gracefully() throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            new ResourceType(graph).name("video");
            transaction.commit();
        }

        MockHttpServletResponse response = getResource("/resource-types/doesnotexist", status().isNotFound());
    }

    @Test
    public void can_create_resourcetype() throws Exception {
        ResourceTypes.CreateResourceTypeCommand command = new ResourceTypes.CreateResourceTypeCommand() {{
            id = URI.create("urn:resource-type:1");
            name = "name";
        }};
        createResource("/resource-types/", command);

        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            assertNotNull(ResourceType.getById(command.id.toString(), graph));
            transaction.rollback();
        }

    }

    @Test
    public void cannot_create_duplicate_resourcetype() throws Exception {
        ResourceTypes.CreateResourceTypeCommand command = new ResourceTypes.CreateResourceTypeCommand() {{
            id = URI.create("urn:resource-type:1");
            name = "name";
        }};
        createResource("/resource-types/", command);
        createResource("/resource-types/", command, status().isConflict());
    }

    @Test
    public void can_delete_resourcetype() throws Exception {
        String id;
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            id = new ResourceType(graph).name("video").getId().toString();
            transaction.commit();
        }
        deleteResource("/resource-types/" + id);
    }

    @Test
    public void can_update_resourcetype() throws Exception {
        URI id;
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            id = new ResourceType(graph).name("video").getId();
            transaction.commit();
        }
        ResourceTypes.UpdateResourceTypeCommand updateCommand = new ResourceTypes.UpdateResourceTypeCommand();
        updateCommand.name = "Audovideo";

        updateResource("/resource-types/" + id, updateCommand);

        Graph graph = factory.create();
        Transaction transaction = graph.tx();
        ResourceType result = ResourceType.getById(id.toString(), graph);
        assertEquals(updateCommand.name, result.getName());
        transaction.rollback();
    }
}

