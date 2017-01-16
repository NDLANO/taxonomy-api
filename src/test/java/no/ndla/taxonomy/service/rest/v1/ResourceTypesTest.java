package no.ndla.taxonomy.service.rest.v1;

import no.ndla.taxonomy.service.GraphFactory;
import no.ndla.taxonomy.service.domain.NotFoundException;
import no.ndla.taxonomy.service.domain.ResourceType;
import no.ndla.taxonomy.service.domain.ResourceTypeSubResourceType;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.URI;
import java.util.Iterator;
import java.util.List;

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

    @Rule
    public ExpectedException exception = ExpectedException.none();

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

        MockHttpServletResponse response = getResource("/v1/resource-types");
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


        MockHttpServletResponse response = getResource("/v1/resource-types/" + id.toString());
        ResourceTypes.ResourceTypeIndexDocument resourceType = getObject(ResourceTypes.ResourceTypeIndexDocument.class, response);
        assertEquals(id, resourceType.id);
    }

    @Test
    public void unknown_resourcetype_fails_gracefully() throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            new ResourceType(graph).name("video");
            transaction.commit();
        }

        MockHttpServletResponse response = getResource("/v1/resource-types/doesnotexist", status().isNotFound());
    }

    @Test
    public void can_create_resourcetype() throws Exception {
        ResourceTypes.CreateResourceTypeCommand command = new ResourceTypes.CreateResourceTypeCommand() {{
            id = URI.create("urn:resource-type:1");
            name = "name";
        }};
        createResource("/v1/resource-types/", command);

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
        createResource("/v1/resource-types/", command);
        createResource("/v1/resource-types/", command, status().isConflict());
    }

    @Test
    public void can_delete_resourcetype() throws Exception {
        String id;
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            id = new ResourceType(graph).name("video").getId().toString();
            transaction.commit();
        }
        deleteResource("/v1/resource-types/" + id);
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

        updateResource("/v1/resource-types/" + id, updateCommand);

        Graph graph = factory.create();
        Transaction transaction = graph.tx();
        ResourceType result = ResourceType.getById(id.toString(), graph);
        assertEquals(updateCommand.name, result.getName());
        transaction.rollback();
    }

    @Test
    public void can_add_subresourcetype_to_resourcetype() throws Exception {
        URI parentIdResourceType;
        URI childIdResourceType = URI.create("urn:resource-type:12");
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            parentIdResourceType = new ResourceType(graph).name("external").getId();
            transaction.commit();
        }
        ResourceTypes.CreateResourceTypeCommand command = new ResourceTypes.CreateResourceTypeCommand() {{
            parentId = parentIdResourceType;
            id = childIdResourceType;
            name = "youtube";
        }};
        final String edgeId = getId(createResource("/v1/resource-types/", command));

        assertNotNull(edgeId);
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            final ResourceType child = ResourceType.getById(childIdResourceType.toString(), graph);
            assertEquals(parentIdResourceType, child.getParent().getId());

            final Iterator<ResourceType> children = ResourceType.getById(parentIdResourceType.toString(), graph).getSubResourceTypes();
            assertAnyTrue(children, r -> childIdResourceType.toString().equals(r.getId().toString()));
            transaction.rollback();
        }
    }

    @Test
    public void cannot_add_existing_subresourcetypes_to_resourcetype() throws Exception {
        URI parentIdResourceType;
        URI childIdResourceType;
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            ResourceType parentResourceType = new ResourceType(graph).name("external");
            ResourceType subResourceType = new ResourceType(graph).name("youtube");
            new ResourceTypeSubResourceType(parentResourceType, subResourceType);
            parentIdResourceType = parentResourceType.getId();
            childIdResourceType = subResourceType.getId();
            transaction.commit();
        }
        ResourceTypes.UpdateResourceTypeCommand command = new ResourceTypes.UpdateResourceTypeCommand() {{
            parentId = parentIdResourceType;
            name = "youtube";
        }};
        updateResource("/v1/resource-types/" + childIdResourceType.toString(), command, status().isConflict());
    }

    @Test
    public void can_remove_parent_from_resourcetype() throws Exception {
        URI childIdResourceType;
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            ResourceType parentResourceType = new ResourceType(graph).name("external");
            ResourceType subResourceType = new ResourceType(graph).name("youtube");
            new ResourceTypeSubResourceType(parentResourceType, subResourceType);
            childIdResourceType = subResourceType.getId();
            transaction.commit();
        }
        ResourceTypes.UpdateResourceTypeCommand command = new ResourceTypes.UpdateResourceTypeCommand() {{
            name = "youtube";
        }};
        updateResource("/v1/resource-types/" + childIdResourceType.toString(), command);

        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            ResourceType resourceType = ResourceType.getById(childIdResourceType.toString(), graph);
            exception.expect(NotFoundException.class);
            resourceType.getParent();
        }
    }

    @Test
    public void can_update_parent_resourcetype() throws Exception {
        URI childIdResourceType, newParentResourceTypeId;
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            ResourceType parentResourceType = new ResourceType(graph).name("external");
            ResourceType subResourceType = new ResourceType(graph).name("youtube");
            ResourceType newParentResourceType = new ResourceType(graph).name("video");
            newParentResourceTypeId = newParentResourceType.getId();
            new ResourceTypeSubResourceType(parentResourceType, subResourceType);
            childIdResourceType = subResourceType.getId();
            transaction.commit();
        }
        ResourceTypes.UpdateResourceTypeCommand command = new ResourceTypes.UpdateResourceTypeCommand() {{
            parentId = newParentResourceTypeId;
        }};
        updateResource("/v1/resource-types/" + childIdResourceType.toString(), command);

        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            final ResourceType resourceType = ResourceType.getById(childIdResourceType.toString(), graph);
            assertEquals("video", resourceType.getParent().getName());
        }
    }

    
}

