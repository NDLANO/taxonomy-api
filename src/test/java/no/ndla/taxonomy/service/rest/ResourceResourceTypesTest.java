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
    public void cannot_have_duplicate_resourcetypes() throws Exception {
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
}
