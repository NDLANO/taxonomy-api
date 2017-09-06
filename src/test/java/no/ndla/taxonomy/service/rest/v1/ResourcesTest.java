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
        builder.subject(s -> s
                .publicId("urn:subject:1")
                .topic(t -> t
                        .publicId("urn:topic:1")
                        .resource(r -> r
                                .name("introduction to trigonometry")
                                .contentUri("urn:article:1")
                                .publicId("urn:resource:1")
                        )));

        MockHttpServletResponse response = getResource("/v1/resources/urn:resource:1");
        Resources.ResourceIndexDocument resource = getObject(Resources.ResourceIndexDocument.class, response);

        assertEquals("introduction to trigonometry", resource.name);
        assertEquals("urn:article:1", resource.contentUri.toString());
        assertEquals("/subject:1/topic:1/resource:1", resource.path);
    }

    @Test
    public void primary_url_is_return_when_getting_single_resource() throws Exception {
        builder.subject(s -> s
                .publicId("urn:subject:1")
                .topic(t -> t
                        .publicId("urn:topic:1")
                        .resource("resource", r -> r
                                .publicId("urn:resource:1")
                        )));
        builder.subject(s -> s
                .publicId("urn:subject:2")
                .topic("primary", t -> t
                        .publicId("urn:topic:2")
                        .resource("resource")
                )
        );

        builder.resource("resource").setPrimaryTopic(builder.topic("primary"));

        MockHttpServletResponse response = getResource("/v1/resources/urn:resource:1");
        Resources.ResourceIndexDocument resource = getObject(Resources.ResourceIndexDocument.class, response);

        assertEquals("/subject:2/topic:2/resource:1", resource.path);
    }

    @Test
    public void resource_without_subject_and_topic_has_no_url() throws Exception {
        builder.resource(r -> r
                .publicId("urn:resource:1")
        );

        MockHttpServletResponse response = getResource("/v1/resources/urn:resource:1");
        Resources.ResourceIndexDocument resource = getObject(Resources.ResourceIndexDocument.class, response);

        assertNull(resource.path);
    }

    @Test
    public void can_get_all_resources() throws Exception {
        builder.subject(s -> s.topic(t -> t.resource(r -> r.name("The inner planets"))));
        builder.subject(s -> s.topic(t -> t.resource(r -> r.name("Gas giants"))));

        MockHttpServletResponse response = getResource("/v1/resources");
        Resources.ResourceIndexDocument[] resources = getObject(Resources.ResourceIndexDocument[].class, response);

        assertEquals(2, resources.length);
        assertAnyTrue(resources, s -> "The inner planets".equals(s.name));
        assertAnyTrue(resources, s -> "Gas giants".equals(s.name));
        assertAllTrue(resources, s -> isValidId(s.id));
        assertAllTrue(resources, r -> !r.path.isEmpty());
    }

    @Test
    public void can_create_resource() throws Exception {
        URI id = getId(createResource("/v1/resources", new Resources.CreateResourceCommand() {{
            name = "testresource";
            contentUri = URI.create("urn:article:1");
        }}));

        Resource resource = resourceRepository.getByPublicId(id);
        assertEquals("testresource", resource.getName());
        assertEquals("urn:article:1", resource.getContentUri().toString());
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
        builder.topic(t -> t
                .resource("resource", r -> r
                        .translation("nb", tr -> tr.name("ressurs"))
                        .resourceType(rt -> rt.name("Learning path"))
                )
        );

        URI id = builder.resource("resource").getPublicId();
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

    @Test
    public void can_get_resource_types() throws Exception {
        builder.resourceType(rt -> rt
                .name("Subject matter")
                .publicId("urn:resourcetype:1")
                .subtype("article", st -> st.name("Article").publicId("urn:resourcetype:2"))
                .subtype("video", st -> st.name("Video").publicId("urn:resourcetype:3"))
        );

        builder.resource(r -> r
                .publicId("urn:resource:1")
                .resourceType("article")
                .resourceType("video")
        );

        MockHttpServletResponse response = getResource("/v1/resources/urn:resource:1/resource-types");
        Resources.ResourceTypeIndexDocument[] result = getObject(Resources.ResourceTypeIndexDocument[].class, response);
        assertEquals(2, result.length);
        assertAnyTrue(result, rt -> rt.name.equals("Article") && rt.id.toString().equals("urn:resourcetype:2") && rt.parentId.toString().equals("urn:resourcetype:1") && rt.connectionId.toString().contains("urn:resource-resourcetype"));
        assertAnyTrue(result, rt -> rt.name.equals("Video") && rt.id.toString().equals("urn:resourcetype:3") && rt.parentId.toString().equals("urn:resourcetype:1"));
    }


    @Test
    public void resources_can_have_same_name() throws Exception {
        builder.resource(r -> r
                .publicId("urn:resource:1")
                .name("What is maths?"));

        Resources.CreateResourceCommand command = new Resources.CreateResourceCommand() {{
            id = URI.create("urn:resource:2");
            name="What is maths?";
        }};

        createResource("/v1/resources", command, status().isCreated());
    }
}
