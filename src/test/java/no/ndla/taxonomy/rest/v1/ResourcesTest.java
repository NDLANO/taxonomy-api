package no.ndla.taxonomy.rest.v1;


import no.ndla.taxonomy.TestSeeder;
import no.ndla.taxonomy.domain.Filter;
import no.ndla.taxonomy.domain.Resource;
import no.ndla.taxonomy.domain.ResourceType;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.rest.v1.commands.CreateResourceCommand;
import no.ndla.taxonomy.rest.v1.commands.UpdateResourceCommand;
import no.ndla.taxonomy.rest.v1.dtos.resources.ParentTopicIndexDocument;
import no.ndla.taxonomy.rest.v1.dtos.resources.ResourceFullIndexDocument;
import no.ndla.taxonomy.rest.v1.dtos.resources.ResourceIndexDocument;
import no.ndla.taxonomy.rest.v1.dtos.resources.ResourceTypeIndexDocument;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;

import static no.ndla.taxonomy.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ResourcesTest extends RestTest {
    @Autowired
    private TestSeeder testSeeder;

    @Test
    public void can_get_single_resource() throws Exception {
        builder.subject(s -> s
                .publicId("urn:subject:1")
                .topic(t -> t
                        .publicId("urn:topic:1")
                        .resource(true, r -> r
                                .name("introduction to trigonometry")
                                .contentUri("urn:article:1")
                                .publicId("urn:resource:1")
                        )));

        MockHttpServletResponse response = testUtils.getResource("/v1/resources/urn:resource:1");
        ResourceIndexDocument resource = testUtils.getObject(ResourceIndexDocument.class, response);

        assertEquals("introduction to trigonometry", resource.name);
        assertEquals("urn:article:1", resource.contentUri.toString());
        assertEquals("/subject:1/topic:1/resource:1", resource.path);

        assertNull(resource.metadata);
    }

    @Test
    public void can_get_single_resource_with_metadata() throws Exception {
        builder.subject(s -> s
                .publicId("urn:subject:1")
                .topic(t -> t
                        .publicId("urn:topic:1")
                        .resource(true, r -> r
                                .name("introduction to trigonometry")
                                .contentUri("urn:article:1")
                                .publicId("urn:resource:1")
                        )));

        final var response = testUtils.getResource("/v1/resources/urn:resource:1?includeMetadata=true");
        final var resource = testUtils.getObject(ResourceIndexDocument.class, response);

        assertEquals("introduction to trigonometry", resource.name);
        assertEquals("urn:article:1", resource.contentUri.toString());
        assertEquals("/subject:1/topic:1/resource:1", resource.path);

        assertTrue(resource.metadata.isVisible());
        assertTrue(resource.metadata.getGrepCodes().size() == 1 && resource.metadata.getGrepCodes().contains("RESOURCE1"));
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
                        .resource("resource", true)
                )
        );

        MockHttpServletResponse response = testUtils.getResource("/v1/resources/urn:resource:1");
        ResourceIndexDocument resource = testUtils.getObject(ResourceIndexDocument.class, response);

        assertEquals("/subject:2/topic:2/resource:1", resource.path);
    }

    @Test
    public void resource_without_subject_and_topic_has_no_url() throws Exception {
        builder.resource(r -> r
                .publicId("urn:resource:1")
        );

        MockHttpServletResponse response = testUtils.getResource("/v1/resources/urn:resource:1");
        ResourceIndexDocument resource = testUtils.getObject(ResourceIndexDocument.class, response);

        assertNull(resource.path);
    }

    @Test
    public void can_get_all_resources() throws Exception {
        builder.subject(s -> s.topic(t -> t.resource(true, r -> r.name("The inner planets"))));
        builder.subject(s -> s.topic(t -> t.resource(true, r -> r.name("Gas giants"))));

        final var allResources = resourceRepository.findAll();

        MockHttpServletResponse response = testUtils.getResource("/v1/resources");
        ResourceIndexDocument[] resources = testUtils.getObject(ResourceIndexDocument[].class, response);

        assertEquals(2, resources.length);
        assertAnyTrue(resources, s -> "The inner planets".equals(s.name));
        assertAnyTrue(resources, s -> "Gas giants".equals(s.name));
        assertAllTrue(resources, s -> isValidId(s.id));
        assertAllTrue(resources, r -> !r.path.isEmpty());

        assertAllTrue(resources, r -> r.metadata == null);
    }

    @Test
    public void can_get_all_resources_with_metadata() throws Exception {
        builder.subject(s -> s.topic(t -> t.resource(true, r -> r.name("The inner planets"))));
        builder.subject(s -> s.topic(t -> t.resource(true, r -> r.name("Gas giants"))));

        MockHttpServletResponse response = testUtils.getResource("/v1/resources?includeMetadata=true");
        ResourceIndexDocument[] resources = testUtils.getObject(ResourceIndexDocument[].class, response);

        assertEquals(2, resources.length);
        assertAnyTrue(resources, s -> "The inner planets".equals(s.name));
        assertAnyTrue(resources, s -> "Gas giants".equals(s.name));
        assertAllTrue(resources, s -> isValidId(s.id));
        assertAllTrue(resources, r -> !r.path.isEmpty());

        assertAllTrue(resources, r -> r.metadata != null);
        assertAllTrue(resources, r -> r.metadata.isVisible());
        assertAllTrue(resources, r -> r.metadata.getGrepCodes().size() == 1);
    }

    @Test
    public void can_create_resource() throws Exception {
        URI id = getId(testUtils.createResource("/v1/resources", new CreateResourceCommand() {{
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

        UpdateResourceCommand command = new UpdateResourceCommand() {{
            name = "The inner planets";
            contentUri = URI.create("urn:article:1");
        }};

        testUtils.updateResource("/v1/resources/" + id, command);

        Resource resource = resourceRepository.getByPublicId(id);
        assertEquals(command.name, resource.getName());
        assertEquals(command.contentUri, resource.getContentUri());
    }

    @Test
    public void can_create_resource_with_id() throws Exception {
        CreateResourceCommand command = new CreateResourceCommand() {{
            id = URI.create("urn:resource:1");
            name = "name";
        }};

        testUtils.createResource("/v1/resources", command);

        assertNotNull(resourceRepository.getByPublicId(command.id));
    }

    @Test
    public void duplicate_ids_not_allowed() throws Exception {
        CreateResourceCommand command = new CreateResourceCommand() {{
            id = URI.create("urn:resource:1");
            name = "name";
        }};

        testUtils.createResource("/v1/resources", command, status().isCreated());
        testUtils.createResource("/v1/resources", command, status().isConflict());
    }

    @Test
    public void can_delete_resource() throws Exception {
        Resource resource = builder.resource(r -> r
                .translation("nb", tr -> tr.name("ressurs"))
                .resourceType(rt -> rt.name("Learning path")));
        resource.getTranslation("nb");

        builder.topic(t -> t
                .resource(resource));

        URI id = builder.resource("resource").getPublicId();
        testUtils.deleteResource("/v1/resources/" + id);
        assertNull(resourceRepository.findByPublicId(id));

        verify(metadataApiService).deleteMetadataByPublicId(id);
    }

    @Test
    public void can_delete_resource_with_two_parent_topics() throws Exception {
        Resource resource = builder.resource("resource");

        builder.topic(child -> child.resource(resource)).name("DELETE EDGE TO ME");
        builder.topic(child -> child.resource(resource)).name("DELETE EDGE TO ME ALSO");

        final var publicId = resource.getPublicId();

        testUtils.deleteResource("/v1/resources/" + publicId);
        assertNull(resourceRepository.findByPublicId(publicId));

        verify(metadataApiService).deleteMetadataByPublicId(publicId);
    }

    @Test
    public void can_get_resource_by_id() throws Exception {
        var resource = newResource();
        resource.setName("The inner planets");
        URI id = resource.getPublicId();

        MockHttpServletResponse response = testUtils.getResource("/v1/resources/" + id);
        ResourceIndexDocument result = testUtils.getObject(ResourceIndexDocument.class, response);

        assertEquals(id, result.id);
    }

    @Test
    public void get_unknown_resource_fails_gracefully() throws Exception {
        testUtils.getResource("/v1/resources/nonexistantid", status().isNotFound());
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

        MockHttpServletResponse response = testUtils.getResource("/v1/resources/urn:resource:1/resource-types");
        ResourceTypeIndexDocument[] result = testUtils.getObject(ResourceTypeIndexDocument[].class, response);
        assertEquals(2, result.length);
        assertAnyTrue(result, rt -> rt.name.equals("Article") && rt.id.toString().equals("urn:resourcetype:2") && rt.parentId.toString().equals("urn:resourcetype:1") && rt.connectionId.toString().contains("urn:resource-resourcetype"));
        assertAnyTrue(result, rt -> rt.name.equals("Video") && rt.id.toString().equals("urn:resourcetype:3") && rt.parentId.toString().equals("urn:resourcetype:1"));
    }

    @Test
    public void resources_can_have_same_name() throws Exception {
        builder.resource(r -> r
                .publicId("urn:resource:1")
                .name("What is maths?"));

        CreateResourceCommand command = new CreateResourceCommand() {{
            id = URI.create("urn:resource:2");
            name = "What is maths?";
        }};

        testUtils.createResource("/v1/resources", command, status().isCreated());
    }

    @Test
    public void get_resource_with_related_topics_filters_resourceTypes() throws Exception {
        final ResourceType resourceType = builder.resourceType(rt -> rt.name("Læringssti").translation("nb", tr -> tr.name("Læringssti")));
        final Filter filter = builder.filter(f -> f.publicId("urn:filter:1").name("Vg 3"));
        final Resource resource = builder.resource(r -> r
                .publicId("urn:resource:1")
                .resourceType(resourceType)
                .filter(filter, builder.relevance(rel -> rel.publicId("urn:relevance:core"))));
        final Topic topic = builder.topic("primary", t -> t
                .name("Philosophy and Mind")
                .publicId("urn:topic:1")
                .contentUri(URI.create("urn:article:6662"))
                .resource(resource, true));

        MockHttpServletResponse response = testUtils.getResource("/v1/resources/" + resource.getPublicId() + "/full");
        ResourceFullIndexDocument result = testUtils.getObject(ResourceFullIndexDocument.class, response);

        assertEquals(resource.getPublicId(), result.id);
        assertEquals(resource.getName(), result.name);
        assertEquals(1, result.resourceTypes.size());
        assertEquals(resourceType.getName(), result.resourceTypes.iterator().next().name);
        assertEquals(1, result.filters.size());
        assertEquals(filter.getName(), result.filters.iterator().next().name);
        assertEquals(1, result.parentTopics.size());
        ParentTopicIndexDocument t = result.parentTopics.iterator().next();
        assertEquals(topic.getName(), t.name);
        assertTrue(t.isPrimary);
        assertEquals(URI.create("urn:article:6662"), t.contentUri);
    }

    @Test
    public void full_resource_has_all_paths() throws Exception {
        testSeeder.resourceInDualSubjectsTestSetup();

        MockHttpServletResponse response = testUtils.getResource("/v1/resources/urn:resource:1/full");
        ResourceFullIndexDocument result = testUtils.getObject(ResourceFullIndexDocument.class, response);
        assertNotNull(result.paths);
        assertEquals(2, result.paths.size());
    }
}
