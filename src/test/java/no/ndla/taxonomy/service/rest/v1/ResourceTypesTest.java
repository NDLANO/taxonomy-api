package no.ndla.taxonomy.service.rest.v1;

import no.ndla.taxonomy.service.domain.ResourceType;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;

import static no.ndla.taxonomy.service.TestUtils.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ResourceTypesTest extends RestTest {

    @Test
    public void can_get_all_resource_types() throws Exception {
        newResourceType().name("video");
        newResourceType().name("audio");

        MockHttpServletResponse response = getResource("/v1/resource-types");
        ResourceTypes.ResourceTypeIndexDocument[] resourcetypes = getObject(ResourceTypes.ResourceTypeIndexDocument[].class, response);
        assertEquals(2, resourcetypes.length);

        assertAnyTrue(resourcetypes, s -> "video".equals(s.name));
        assertAnyTrue(resourcetypes, s -> "audio".equals(s.name));
        assertAllTrue(resourcetypes, s -> isValidId(s.id));
    }

    @Test
    public void can_get_resourcetype_by_id() throws Exception {
        URI id = newResourceType().name("video").getPublicId();

        MockHttpServletResponse response = getResource("/v1/resource-types/" + id.toString());
        ResourceTypes.ResourceTypeIndexDocument resourceType = getObject(ResourceTypes.ResourceTypeIndexDocument.class, response);
        assertEquals(id, resourceType.id);
    }

    @Test
    public void unknown_resourcetype_fails_gracefully() throws Exception {
        getResource("/v1/resource-types/doesnotexist", status().isNotFound());
    }

    @Test
    public void can_create_resourcetype() throws Exception {
        ResourceTypes.CreateResourceTypeCommand command = new ResourceTypes.CreateResourceTypeCommand() {{
            id = URI.create("urn:resource-type:1");
            name = "name";
        }};

        createResource("/v1/resource-types/", command);

        ResourceType result = resourceTypeRepository.getByPublicId(command.id);
        assertEquals(command.name, result.getName());
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
        URI id = newResourceType().name("video").getPublicId();
        deleteResource("/v1/resource-types/" + id);
        assertNull(resourceTypeRepository.findByPublicId(id));
    }

    @Test
    public void can_update_resourcetype() throws Exception {
        URI id = newResourceType().name("video").getPublicId();
        ResourceTypes.UpdateResourceTypeCommand updateCommand = new ResourceTypes.UpdateResourceTypeCommand() {{
            name = "Audovideo";
        }};

        updateResource("/v1/resource-types/" + id, updateCommand);

        ResourceType result = resourceTypeRepository.getByPublicId(id);
        assertEquals(updateCommand.name, result.getName());
    }

    @Test
    public void can_add_subresourcetype_to_resourcetype() throws Exception {
        ResourceType parent = newResourceType().name("external");

        URI childId = getId(createResource("/v1/resource-types/", new ResourceTypes.CreateResourceTypeCommand() {{
            parentId = parent.getPublicId();
            name = "youtube";
        }}));

        ResourceType child = resourceTypeRepository.getByPublicId(childId);
        assertEquals(parent.getPublicId(), child.getParent().getPublicId());
    }

    @Test
    public void can_remove_parent_from_resourcetype() throws Exception {
        ResourceType parent = newResourceType().name("external");
        ResourceType child = newResourceType().name("youtube");
        child.setParent(parent);

        URI childId = child.getPublicId();

        updateResource("/v1/resource-types/" + childId, new ResourceTypes.UpdateResourceTypeCommand() {{
            parentId = null;
            name = child.getName();
        }});

        assertNull(resourceTypeRepository.getByPublicId(childId).getParent());
    }

    @Test
    public void can_update_parent_resourcetype() throws Exception {
        ResourceType oldParent = newResourceType().name("external");
        ResourceType child = newResourceType().name("youtube").parent(oldParent);
        ResourceType newParent = newResourceType().name("video");

        updateResource("/v1/resource-types/" + child.getPublicId(), new ResourceTypes.UpdateResourceTypeCommand() {{
            parentId = newParent.getPublicId();
            name = child.getName();
        }});

        assertEquals("video", child.getParent().getName());
    }

    @Test
    public void can_get_subresourcetypes() throws Exception {
        ResourceType parent = newResourceType().name("external");
        newResourceType().name("youtube").parent(parent);
        newResourceType().name("ted").parent(parent);
        newResourceType().name("vimeo").parent(parent);

        MockHttpServletResponse response = getResource("/v1/resource-types/" + parent.getPublicId() + "/subresourcetypes");
        ResourceTypes.ResourceTypeIndexDocument[] subResourceTypes = getObject(ResourceTypes.ResourceTypeIndexDocument[].class, response);

        assertEquals(3, subResourceTypes.length);
        assertAnyTrue(subResourceTypes, t -> "youtube".equals(t.name));
        assertAnyTrue(subResourceTypes, t -> "ted".equals(t.name));
        assertAnyTrue(subResourceTypes, t -> "vimeo".equals(t.name));
        assertAllTrue(subResourceTypes, t -> isValidId(t.id));
    }

    @Test
    public void can_get_subresourcetypes_recursively() throws Exception {
        ResourceType parent = newResourceType().name("external");
        final ResourceType video = newResourceType().name("video").parent(parent);
        final ResourceType youtube = newResourceType().name("youtube").parent(video);

        MockHttpServletResponse response = getResource("/v1/resource-types/" + parent.getPublicId() + "/subresourcetypes?recursive=true");
        ResourceTypes.ResourceTypeIndexDocument[] subResourceTypes = getObject(ResourceTypes.ResourceTypeIndexDocument[].class, response);

        assertEquals(1, subResourceTypes.length);
        assertEquals("video", subResourceTypes[0].name);
        assertEquals("youtube", subResourceTypes[0].subResourceTypes[0].name);
    }
}

