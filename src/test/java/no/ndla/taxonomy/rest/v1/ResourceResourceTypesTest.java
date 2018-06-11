package no.ndla.taxonomy.rest.v1;

import no.ndla.taxonomy.domain.Resource;
import no.ndla.taxonomy.domain.ResourceResourceType;
import no.ndla.taxonomy.domain.ResourceType;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;

import static junit.framework.TestCase.assertEquals;
import static no.ndla.taxonomy.TestUtils.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ResourceResourceTypesTest extends RestTest {

    @Test
    public void can_add_resourcetype_to_resource() throws Exception {
        URI integrationResourceId = newResource().name("Introduction to integration").getPublicId();
        URI textTypeId = newResourceType().name("text").getPublicId();

        URI id = getId(createResource("/v1/resource-resourcetypes", new ResourceResourceTypes.CreateResourceResourceTypeCommand() {{
            resourceId = integrationResourceId;
            resourceTypeId = textTypeId;
        }}));

        Resource resource = resourceRepository.getByPublicId(integrationResourceId);
        assertEquals(1, count(resource.getResourceTypes()));
        assertAnyTrue(resource.getResourceTypes(), t -> "text".equals(t.getName()));
        assertNotNull(resourceResourceTypeRepository.getByPublicId(id));
    }

    @Test
    public void cannot_have_duplicate_resourcetypes_for_resource() throws Exception {
        Resource integrationResource = newResource().name("Introduction to integration");
        ResourceType resourceType = newResourceType().name("text");
        save(integrationResource.addResourceType(resourceType));

        createResource("/v1/resource-resourcetypes", new ResourceResourceTypes.CreateResourceResourceTypeCommand() {{
            resourceId = integrationResource.getPublicId();
            resourceTypeId = resourceType.getPublicId();
        }}, status().isConflict());
    }

    @Test
    public void can_delete_resource_resourcetype() throws Exception {
        Resource integrationResource = builder.resource(r -> r.name("Introduction to integration"));
        ResourceType resourceType = builder.resourceType(rt -> rt.name("text"));
        URI id = save(integrationResource.addResourceType(resourceType)).getPublicId();

        deleteResource("/v1/resource-resourcetypes/" + id);
        assertNull(resourceResourceTypeRepository.findByPublicId(id));
    }

    @Test
    public void can_list_all_resource_resourcetypes() throws Exception {
        Resource trigonometry = newResource().name("Advanced trigonometry");
        ResourceType article = newResourceType().name("article");
        save(trigonometry.addResourceType(article));

        Resource integration = newResource().name("Introduction to integration");
        ResourceType text = newResourceType().name("text");
        save(integration.addResourceType(text));

        MockHttpServletResponse response = getResource("/v1/resource-resourcetypes");
        ResourceResourceTypes.ResourceResourceTypeIndexDocument[] resourceResourcetypes = getObject(ResourceResourceTypes.ResourceResourceTypeIndexDocument[].class, response);
        assertEquals(2, resourceResourcetypes.length);
        assertAnyTrue(resourceResourcetypes, t -> trigonometry.getPublicId().equals(t.resourceId) && article.getPublicId().equals(t.resourceTypeId));
        assertAnyTrue(resourceResourcetypes, t -> integration.getPublicId().equals(t.resourceId) && text.getPublicId().equals(t.resourceTypeId));
    }

    @Test
    public void can_get_a_resource_resourcetype() throws Exception {
        Resource resource = newResource().name("Advanced trigonometry");
        ResourceType resourceType = newResourceType().name("article");
        ResourceResourceType resourceResourceType = resource.addResourceType(resourceType);
        URI id = save(resourceResourceType).getPublicId();

        MockHttpServletResponse response = getResource("/v1/resource-resourcetypes/" + id);
        ResourceResourceTypes.ResourceResourceTypeIndexDocument result = getObject(ResourceResourceTypes.ResourceResourceTypeIndexDocument.class, response);

        assertEquals(resource.getPublicId(), result.resourceId);
        assertEquals(resourceType.getPublicId(), result.resourceTypeId);
    }

    @Test
    public void can_change_id_of_resource_type() throws Exception {
        Resource resource = newResource().name("Advanced trigonometry");
        ResourceType resourceType = newResourceType().name("article");
        ResourceResourceType resourceResourceType = resource.addResourceType(resourceType);
        URI id = save(resourceResourceType).getPublicId();

        resourceType.setPublicId(URI.create("urn:resourcetype:article"));

        MockHttpServletResponse response = getResource("/v1/resource-resourcetypes/" + id);
        ResourceResourceTypes.ResourceResourceTypeIndexDocument result = getObject(ResourceResourceTypes.ResourceResourceTypeIndexDocument.class, response);

        assertEquals(resource.getPublicId(), result.resourceId);
        assertEquals(URI.create("urn:resourcetype:article"), result.resourceTypeId);
    }
}
