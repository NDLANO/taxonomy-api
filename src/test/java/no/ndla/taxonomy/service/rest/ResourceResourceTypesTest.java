package no.ndla.taxonomy.service.rest;

import no.ndla.taxonomy.service.GraphFactory;
import no.ndla.taxonomy.service.domain.Resource;
import no.ndla.taxonomy.service.domain.ResourceResourceType;
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

import static junit.framework.TestCase.assertEquals;
import static no.ndla.taxonomy.service.TestUtils.*;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("junit")
public class ResourceResourceTypesTest {

    @Autowired
    private GraphFactory factory;

    @Before
    public void setup() throws Exception {
        clearGraph();
    }

    @Test
    public void can_add_resourcetype_to_resource() throws Exception {
        URI integrationResourceId;
        URI textTypeId;

        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            integrationResourceId = new Resource(graph).name("Introduction to integration").getId();
            textTypeId = new ResourceType(graph).name("text").getId();
            transaction.commit();
        }

        String id = getId(createResource("/resource-resourcetypes", new ResourceResourceTypes.CreateResourceResourceTypeCommand() {{
            resourceId = integrationResourceId;
            resourceTypeId = textTypeId;
        }}));

        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            Resource resource = Resource.getById(integrationResourceId.toString(), graph);
            assertEquals(1, count(resource.getResourceTypes()));
            assertAnyTrue(resource.getResourceTypes(), t -> "text".equals(t.getName()));
            assertNotNull(ResourceResourceType.getById(id, graph));
            transaction.rollback();
        }
    }

    @Test
    public void cannot_have_duplicate_resourcetypes_for_resource() throws Exception {
        Resource integrationResource;
        ResourceType resourceType;

        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            integrationResource = new Resource(graph).name("Introduction to integration");
            resourceType = new ResourceType(graph).name("text");
            integrationResource.addResourceType(resourceType);
            transaction.commit();
        }

        createResource("/resource-resourcetypes", new ResourceResourceTypes.CreateResourceResourceTypeCommand() {{
            resourceId = integrationResource.getId();
            resourceTypeId = resourceType.getId();
        }}, status().isConflict());
    }

    @Test
    public void can_delete_resource_resourcetype() throws Exception {
        final ResourceResourceType resourceResourceType;
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            Resource integrationResource = new Resource(graph).name("Introduction to integration");
            ResourceType resourceType = new ResourceType(graph).name("text");
            resourceResourceType = integrationResource.addResourceType(resourceType);
            transaction.commit();
        }

        deleteResource("/resource-resourcetypes/" + resourceResourceType.getId().toString());
    }

    @Test
    public void can_list_all_resource_resourcetypes() throws Exception {
        ResourceResourceType trigonometryArticle;
        ResourceResourceType integrationText;
        URI trigId, articleId, integrationId, textId;
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            Resource resource = new Resource(graph).name("Advanced trigonometry");
            trigId = resource.getId();
            ResourceType resourceType = new ResourceType(graph).name("article");
            articleId = resourceType.getId();
            trigonometryArticle = resource.addResourceType(resourceType);

            final Resource integrationResource = new Resource(graph).name("Introduction to integration");
            integrationId = integrationResource.getId();
            final ResourceType textType = new ResourceType(graph).name("text");
            textId = textType.getId();
            integrationText = integrationResource.addResourceType(textType);

            transaction.commit();
        }

        final MockHttpServletResponse response = getResource("/resource-resourcetypes");
        final ResourceResourceTypes.ResourceResourceTypeIndexDocument[] resourceResourcetypes = getObject(ResourceResourceTypes.ResourceResourceTypeIndexDocument[].class, response);
        assertEquals(2, resourceResourcetypes.length);
        assertAnyTrue(resourceResourcetypes, t -> trigId.equals(t.resourceId) && articleId.equals(t.resourceTypeId));
        assertAnyTrue(resourceResourcetypes, t -> integrationId.equals(t.resourceId) && textId.equals(t.resourceTypeId));
    }
}
