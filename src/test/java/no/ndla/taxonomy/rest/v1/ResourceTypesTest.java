package no.ndla.taxonomy.rest.v1;

import no.ndla.taxonomy.domain.ResourceType;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;

import static no.ndla.taxonomy.TestUtils.*;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ResourceTypesTest extends RestTest {

    @Test
    public void can_get_all_resource_types() throws Exception {
        builder.resourceType(rt -> rt
                .name("video")
                .subtype(st -> st.name("lecture"))
        );
        builder.resourceType(rt -> rt.name("audio"));

        MockHttpServletResponse response = getResource("/v1/resource-types");
        ResourceTypes.ResourceTypeIndexDocument[] resourcetypes = getObject(ResourceTypes.ResourceTypeIndexDocument[].class, response);

        assertEquals(2, resourcetypes.length);
        assertAnyTrue(resourcetypes, s -> "video".equals(s.name) && s.subtypes.get(0).name.equals("lecture"));
        assertAnyTrue(resourcetypes, s -> "audio".equals(s.name));
        assertAllTrue(resourcetypes, s -> isValidId(s.id));
    }

    @Test
    public void can_get_resourcetype_by_id() throws Exception {
        URI id = builder.resourceType(rt -> rt.name("video")).getPublicId();

        MockHttpServletResponse response = getResource("/v1/resource-types/" + id.toString());
        ResourceTypes.ResourceTypeIndexDocument resourceType = getObject(ResourceTypes.ResourceTypeIndexDocument.class, response);
        assertEquals(id, resourceType.id);
        assertEquals(0, resourceType.subtypes.size());
    }

    @Test
    public void can_get_all_resource_types_with_translation() throws Exception {
        builder.resourceType(rt -> rt.name("video").translation("nb", tr -> tr.name("film")));
        builder.resourceType(rt -> rt.name("audio").translation("nb", tr -> tr.name("lydklipp")));

        MockHttpServletResponse response = getResource("/v1/resource-types?language=nb");
        ResourceTypes.ResourceTypeIndexDocument[] resourcetypes = getObject(ResourceTypes.ResourceTypeIndexDocument[].class, response);

        assertEquals(2, resourcetypes.length);
        assertAnyTrue(resourcetypes, s -> "film".equals(s.name));
        assertAnyTrue(resourcetypes, s -> "lydklipp".equals(s.name));
    }

    @Test
    public void can_get_resourcetype_by_id_with_translation() throws Exception {
        URI id = builder.resourceType(rt -> rt.name("video").translation("nb", tr -> tr.name("film"))).getPublicId();

        MockHttpServletResponse response = getResource("/v1/resource-types/" + id.toString() + "?language=nb");
        ResourceTypes.ResourceTypeIndexDocument resourceType = getObject(ResourceTypes.ResourceTypeIndexDocument.class, response);
        assertEquals("film", resourceType.name);
    }

    @Test
    public void unknown_resourcetype_fails_gracefully() throws Exception {
        getResource("/v1/resource-types/doesnotexist", status().isNotFound());
    }

    @Test
    public void can_create_resourcetype() throws Exception {
        ResourceTypes.CreateResourceTypeCommand command = new ResourceTypes.CreateResourceTypeCommand() {{
            id = URI.create("urn:resourcetype:1");
            name = "name";
        }};

        createResource("/v1/resource-types/", command);

        ResourceType result = resourceTypeRepository.getByPublicId(command.id);
        assertEquals(command.name, result.getName());
    }

    @Test
    public void cannot_create_duplicate_resourcetype() throws Exception {
        ResourceTypes.CreateResourceTypeCommand command = new ResourceTypes.CreateResourceTypeCommand() {{
            id = URI.create("urn:resourcetype:1");
            name = "name";
        }};
        createResource("/v1/resource-types/", command);
        createResource("/v1/resource-types/", command, status().isConflict());
    }

    @Test
    public void can_delete_resourcetype() throws Exception {
        URI id = builder.resourceType(rt -> rt.name("video")).getPublicId();
        deleteResource("/v1/resource-types/" + id);
        assertNull(resourceTypeRepository.findByPublicId(id));
    }

    @Test
    public void can_update_resourcetype() throws Exception {
        URI id = builder.resourceType(rt -> rt.name("video")).getPublicId();

        updateResource("/v1/resource-types/" + id, new ResourceTypes.UpdateResourceTypeCommand() {{
            name = "Audiovideo";
        }});

        ResourceType result = resourceTypeRepository.getByPublicId(id);
        assertEquals("Audiovideo", result.getName());
    }

    @Test
    public void can_change_resource_type_id() throws Exception {
        URI id = builder.resourceType(rt -> rt.name("video")).getPublicId();

        updateResource("/v1/resource-types/" + id, new ResourceTypes.UpdateResourceTypeCommand() {{
            name = "Audiovideo";
            id = URI.create("urn:resourcetype:audiovideo");
        }});

        ResourceType result = resourceTypeRepository.getByPublicId(URI.create("urn:resourcetype:audiovideo"));
        assertEquals("urn:resourcetype:audiovideo", result.getPublicId().toString());
    }

    @Test
    public void can_add_subresourcetype_to_resourcetype() throws Exception {
        ResourceType parent = builder.resourceType(rt -> rt.name("external"));

        URI childId = getId(createResource("/v1/resource-types/", new ResourceTypes.CreateResourceTypeCommand() {{
            parentId = parent.getPublicId();
            name = "youtube";
        }}));

        ResourceType child = resourceTypeRepository.getByPublicId(childId);
        assertEquals(parent.getPublicId(), child.getParent().get().getPublicId());
    }

    @Test
    public void can_remove_parent_from_resourcetype() throws Exception {
        ResourceType parent = builder.resourceType(rt -> rt.name("external"));
        ResourceType child = builder.resourceType(rt -> rt.name("youtube"));
        child.setParent(parent);

        URI childId = child.getPublicId();

        updateResource("/v1/resource-types/" + childId, new ResourceTypes.UpdateResourceTypeCommand() {{
            parentId = null;
            name = child.getName();
        }});

        assertFalse(resourceTypeRepository.getByPublicId(childId).getParent().isPresent());
    }

    @Test
    public void can_update_parent_resourcetype() throws Exception {
        ResourceType oldParent = builder.resourceType(rt -> rt.name("external"));
        ResourceType child = builder.resourceType(rt -> rt.name("youtube"));
        child.setParent(oldParent);
        URI newParentId = builder.resourceType(rt -> rt.name("video")).getPublicId();

        updateResource("/v1/resource-types/" + child.getPublicId(), new ResourceTypes.UpdateResourceTypeCommand() {{
            parentId = newParentId;
            name = child.getName();
        }});

        assertEquals("video", child.getParent().get().getName());
    }

    @Test
    public void can_get_subtypes() throws Exception {
        URI id = builder.resourceType(rt -> rt
                .name("external")
                .subtype(st -> st.name("youtube"))
                .subtype(st -> st.name("ted"))
                .subtype(st -> st.name("vimeo"))
        ).getPublicId();

        MockHttpServletResponse response = getResource("/v1/resource-types/" + id + "/subtypes");
        ResourceTypes.ResourceTypeIndexDocument[] subResourceTypes = getObject(ResourceTypes.ResourceTypeIndexDocument[].class, response);

        assertEquals(3, subResourceTypes.length);
        assertAnyTrue(subResourceTypes, t -> "youtube".equals(t.name));
        assertAnyTrue(subResourceTypes, t -> "ted".equals(t.name));
        assertAnyTrue(subResourceTypes, t -> "vimeo".equals(t.name));
        assertAllTrue(subResourceTypes, t -> isValidId(t.id));
    }

    @Test
    public void can_get_subtypes_recursively() throws Exception {
        URI id = builder.resourceType(rt -> rt
                .name("external")
                .subtype(st -> st.name("video")
                        .subtype(st2 -> st2.name("youtube"))
                )
        ).getPublicId();

        MockHttpServletResponse response = getResource("/v1/resource-types/" + id + "/subtypes?recursive=true");
        ResourceTypes.ResourceTypeIndexDocument[] subResourceTypes = getObject(ResourceTypes.ResourceTypeIndexDocument[].class, response);

        assertEquals(1, subResourceTypes.length);
        assertEquals("video", subResourceTypes[0].name);
        assertEquals("youtube", subResourceTypes[0].subtypes.get(0).name);
    }
}

