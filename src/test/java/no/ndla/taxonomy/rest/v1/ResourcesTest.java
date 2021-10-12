/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;


import no.ndla.taxonomy.TestSeeder;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.domain.Resource;
import no.ndla.taxonomy.domain.ResourceType;
import no.ndla.taxonomy.rest.v1.commands.ResourceCommand;
import no.ndla.taxonomy.service.dtos.*;
import org.junit.jupiter.api.BeforeEach;
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

    @BeforeEach
    void clearAllRepos() {
        resourceRepository.deleteAllAndFlush();
        nodeRepository.deleteAllAndFlush();
    }

    @Test
    public void can_get_single_resource() throws Exception {
        builder.node(s -> s
                .nodeType(NodeType.SUBJECT)
                .isContext(true)
                .publicId("urn:subject:1")
                .child(t -> t
                        .nodeType(NodeType.TOPIC)
                        .publicId("urn:topic:1")
                        .resource(true, r -> r
                                .name("introduction to trigonometry")
                                .contentUri("urn:article:1")
                                .publicId("urn:resource:1")
                        )));

        final var response = testUtils.getResource("/v1/resources/urn:resource:1");
        final var resource = testUtils.getObject(ResourceDTO.class, response);

        assertEquals("introduction to trigonometry", resource.getName());
        assertEquals("urn:article:1", resource.getContentUri().toString());
        assertEquals("/subject:1/topic:1/resource:1", resource.getPath());

        assertTrue(resource.getMetadata().isVisible());
        assertTrue(resource.getMetadata().getGrepCodes().size() == 1 && resource.getMetadata().getGrepCodes().contains("RESOURCE1"));
    }

    @Test
    public void primary_url_is_return_when_getting_single_resource() throws Exception {
        builder.node(NodeType.SUBJECT, s -> s
                .isContext(true)
                .publicId("urn:subject:1")
                .child(NodeType.TOPIC, t -> t
                        .publicId("urn:topic:1")
                        .resource("resource", r -> r
                                .publicId("urn:resource:1")
                        )));
        builder.node(NodeType.SUBJECT, s -> s
                .isContext(true)
                .publicId("urn:subject:2")
                .child("primary", NodeType.TOPIC, t -> t
                        .publicId("urn:topic:2")
                        .resource("resource", true)
                )
        );

        MockHttpServletResponse response = testUtils.getResource("/v1/resources/urn:resource:1");
        final var resource = testUtils.getObject(ResourceDTO.class, response);

        assertEquals("/subject:2/topic:2/resource:1", resource.getPath());
    }

    @Test
    public void resource_without_subject_and_topic_has_no_url() throws Exception {
        builder.resource(r -> r
                .publicId("urn:resource:1")
        );

        MockHttpServletResponse response = testUtils.getResource("/v1/resources/urn:resource:1");
        final var resource = testUtils.getObject(ResourceDTO.class, response);

        assertNull(resource.getPath());
    }

    @Test
    public void can_get_all_resources() throws Exception {
        builder.node(s -> s.nodeType(NodeType.SUBJECT).isContext(true).child(t -> t.nodeType(NodeType.TOPIC).resource(true, r -> r.name("The inner planets"))));
        builder.node(s -> s.nodeType(NodeType.SUBJECT).isContext(true).child(t -> t.nodeType(NodeType.TOPIC).resource(true, r -> r.name("Gas giants"))));

        MockHttpServletResponse response = testUtils.getResource("/v1/resources");
        final var resources = testUtils.getObject(ResourceDTO[].class, response);

        assertEquals(2, resources.length);
        assertAnyTrue(resources, s -> "The inner planets".equals(s.getName()));
        assertAnyTrue(resources, s -> "Gas giants".equals(s.getName()));
        assertAllTrue(resources, s -> isValidId(s.getId()));
        assertAllTrue(resources, r -> !r.getPath().isEmpty());

        assertAllTrue(resources, r -> r.getMetadata() != null);
        assertAllTrue(resources, r -> r.getMetadata().isVisible());
        assertAllTrue(resources, r -> r.getMetadata().getGrepCodes().size() == 1);
    }

    @Test
    public void can_get_resources_by_contentURI() throws Exception {
        builder.node(s -> s
                .nodeType(NodeType.SUBJECT)
                .isContext(true)
                .child(t -> t.resource(true, r -> {
                    r.name("The inner planets");
                    r.contentUri("urn:test:1");
                }))
        );

        builder.node(s -> s
                .nodeType(NodeType.SUBJECT)
                .isContext(true)
                .child(t -> t.resource(true, r -> {
                    r.name("Gas giants");
                    r.contentUri("urn:test:2");
            }))
        );

        {
            final var response = testUtils.getResource("/v1/resources?contentURI=urn:test:1");
            final var resources = testUtils.getObject(ResourceDTO[].class, response);

            assertEquals(1, resources.length);
            assertEquals("The inner planets", resources[0].getName());
        }

        {
            final var response = testUtils.getResource("/v1/resources?contentURI=urn:test:2");
            final var resources = testUtils.getObject(ResourceDTO[].class, response);

            assertEquals(1, resources.length);
            assertEquals("Gas giants", resources[0].getName());
        }
    }

    @Test
    public void can_create_resource() throws Exception {
        URI id = getId(testUtils.createResource("/v1/resources", new ResourceCommand() {{
            name = "testresource";
            contentUri = URI.create("urn:article:1");
        }}));

        Resource resource = resourceRepository.getByPublicId(id);
        assertEquals("testresource", resource.getName());
        assertEquals("urn:article:1", resource.getContentUri().toString());
    }

    @Test
    public void can_create_resource_with_id() throws Exception {
        final var command = new ResourceCommand() {{
            id = URI.create("urn:resource:1");
            name = "name";
        }};

        testUtils.createResource("/v1/resources", command);

        assertNotNull(resourceRepository.getByPublicId(command.id));
    }

    @Test
    public void can_update_resource() throws Exception {
        URI publicId = newResource().getPublicId();

        final var command = new ResourceCommand() {{
            id = publicId;
            name = "The inner planets";
            contentUri = URI.create("urn:article:1");
        }};

        testUtils.updateResource("/v1/resources/" + publicId, command);

        Resource resource = resourceRepository.getByPublicId(publicId);
        assertEquals(command.name, resource.getName());
        assertEquals(command.contentUri, resource.getContentUri());
    }

    @Test
    public void can_update_resource_with_new_id() throws Exception {
        URI publicId = newResource().getPublicId();
        URI randomId = URI.create("uri:resource:random");

        final var command = new ResourceCommand() {{
            id = randomId;
            name = "The inner planets";
            contentUri = URI.create("urn:article:1");
        }};

        testUtils.updateResource("/v1/resources/" + publicId, command);

        Resource resource = resourceRepository.getByPublicId(randomId);
        assertEquals(command.name, resource.getName());
        assertEquals(command.contentUri, resource.getContentUri());
    }

    @Test
    public void duplicate_ids_not_allowed() throws Exception {
        final var command = new ResourceCommand() {{
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

        builder.node(t -> t
                .nodeType(NodeType.TOPIC)
                .resource(resource));

        URI id = builder.resource("resource").getPublicId();
        testUtils.deleteResource("/v1/resources/" + id);
        assertNull(resourceRepository.findByPublicId(id));

        verify(metadataApiService).deleteMetadataByPublicId(id);
    }

    @Test
    public void can_delete_resource_with_two_parent_topics() throws Exception {
        Resource resource = builder.resource("resource");

        builder.node(child -> child.nodeType(NodeType.TOPIC).resource(resource)).name("DELETE EDGE TO ME");
        builder.node(child -> child.nodeType(NodeType.TOPIC).resource(resource)).name("DELETE EDGE TO ME ALSO");

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
        final var result = testUtils.getObject(ResourceDTO.class, response);

        assertEquals(id, result.getId());
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
        final var result = testUtils.getObject(ResourceTypeWithConnectionDTO[].class, response);
        assertEquals(2, result.length);
        assertAnyTrue(result, rt -> rt.getName().equals("Article") && rt.getId().toString().equals("urn:resourcetype:2") && rt.getParentId().toString().equals("urn:resourcetype:1") && rt.getConnectionId().toString().contains("urn:resource-resourcetype"));
        assertAnyTrue(result, rt -> rt.getName().equals("Video") && rt.getId().toString().equals("urn:resourcetype:3") && rt.getParentId().toString().equals("urn:resourcetype:1"));
    }

    @Test
    public void resources_can_have_same_name() throws Exception {
        builder.resource(r -> r
                .publicId("urn:resource:1")
                .name("What is maths?"));

        final var command = new ResourceCommand() {{
            id = URI.create("urn:resource:2");
            name = "What is maths?";
        }};

        testUtils.createResource("/v1/resources", command, status().isCreated());
    }

    @Test
    public void get_resource_with_related_topics_filters_resourceTypes() throws Exception {
        final ResourceType resourceType = builder.resourceType(rt -> rt.name("Læringssti").translation("nb", tr -> tr.name("Læringssti")));
        final Resource resource = builder.resource(r -> r
                .publicId("urn:resource:1")
                .resourceType(resourceType));
        final Node topic = builder.node("primary", NodeType.TOPIC, t -> t
                .name("Philosophy and Mind")
                .publicId("urn:topic:1")
                .contentUri(URI.create("urn:article:6662"))
                .resource(resource, true));

        MockHttpServletResponse response = testUtils.getResource("/v1/resources/" + resource.getPublicId() + "/full");
        final var result = testUtils.getObject(ResourceWithParentNodesDTO.class, response);

        assertEquals(resource.getPublicId(), result.getId());
        assertEquals(resource.getName(), result.getName());
        assertEquals(1, result.getResourceTypes().size());
        assertEquals(resourceType.getName(), result.getResourceTypes().iterator().next().getName());
        assertEquals(0, result.getFilters().size());
        assertEquals(1, result.getParentNodes().size());
        final NodeWithResourceConnectionDTO t = result.getParentNodes().iterator().next();
        assertEquals(topic.getName(), t.getName());
        assertTrue(t.isPrimary());
        assertEquals(URI.create("urn:article:6662"), t.getContentUri());
    }

    @Test
    public void full_resource_has_all_paths() throws Exception {
        testSeeder.resourceInDualSubjectsTestSetup();

        MockHttpServletResponse response = testUtils.getResource("/v1/resources/urn:resource:1/full");
        final var result = testUtils.getObject(ResourceWithParentTopicsDTO.class, response);
        assertNotNull(result.getPaths());
        assertEquals(2, result.getPaths().size());
    }

    @Test
    public void resource_has_all_paths() throws Exception {
        testSeeder.resourceInDualSubjectsTestSetup();

        MockHttpServletResponse response = testUtils.getResource("/v1/resources/urn:resource:1");
        final var result = testUtils.getObject(ResourceWithParentTopicsDTO.class, response);
        assertNotNull(result.getPaths());
        assertEquals(2, result.getPaths().size());
    }

    @Test
    public void can_get_resource_connection_id() throws Exception {
        Node topic = builder.node(t -> t
                .nodeType(NodeType.TOPIC)
                .publicId("urn:topic:1")
                .resource()
        );
        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:1/resources");
        final var result = testUtils.getObject(ResourceWithTopicConnectionDTO[].class, response);

        assertEquals(first(topic.getNodeResources()).getPublicId(), result[0].getConnectionId());
    }

    @Test
    public void can_get_resource_connections_with_metadata() throws Exception {
        Node topic = builder.node(t -> t
                .nodeType(NodeType.TOPIC)
                .publicId("urn:topic:1")
                .resource()
        );
        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:1/resources");
        final var result = testUtils.getObject(ResourceWithTopicConnectionDTO[].class, response);

        assertEquals(first(topic.getNodeResources()).getPublicId(), result[0].getConnectionId());
        assertAllTrue(result, connection -> connection.getMetadata() != null);
        assertAllTrue(result, connection -> connection.getMetadata().isVisible());
        assertAllTrue(result, connection -> connection.getMetadata().getGrepCodes().size() == 1);
    }

    @Test
    public void can_get_resource_connection_id_recursively() throws Exception {
        builder.node("topic", NodeType.TOPIC, t -> t
                .publicId("urn:topic:1343")
                .resource(r -> r
                        .name("a")
                        .publicId("urn:resource:1"))
                .child("subtopic", NodeType.TOPIC, st -> st
                        .publicId("urn:topic:2")
                        .resource(r -> r.name("b")
                                .publicId("urn:resource:2")))
        );

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:1343/resources?recursive=true");
        final var result = testUtils.getObject(ResourceWithTopicConnectionDTO[].class, response);

        assertEquals(first(builder.node("topic").getNodeResources()).getPublicId(), result[0].getConnectionId());
        assertEquals(first(builder.node("subtopic").getNodeResources()).getPublicId(), result[1].getConnectionId());
    }

    @Test
    public void can_get_resources_for_a_topic_recursively() throws Exception {
        builder.node(s -> s
                .nodeType(NodeType.SUBJECT)
                .isContext(true)
                .publicId("urn:subject:1")
                .name("subject a")
                .child(t -> t
                        .nodeType(NodeType.TOPIC)
                        .publicId("urn:topic:a")
                        .name("a")
                        .resource(true, r -> r
                                .publicId("urn:resource:1")
                                .name("resource a").contentUri("urn:article:a"))
                        .child(st -> st
                                .nodeType(NodeType.TOPIC)
                                .publicId("urn:topic:a:1")
                                .name("aa")
                                .resource(true, r -> r.name("resource aa").contentUri("urn:article:aa"))
                                .child(st2 -> st2
                                        .nodeType(NodeType.TOPIC)
                                        .publicId("urn:topic:a:1:1")
                                        .name("aaa")
                                        .resource(true, r -> r.name("resource aaa").contentUri("urn:article:aaa"))
                                )
                                .child(st2 -> st2
                                        .nodeType(NodeType.TOPIC)
                                        .publicId("urn:topic:a:1:2")
                                        .name("aab")
                                        .resource(true, r -> r.name("resource aab").contentUri("urn:article:aab"))
                                )
                        )
                ));

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:a/resources?recursive=true");
        final var result = testUtils.getObject(ResourceWithTopicConnectionDTO[].class, response);

        assertEquals(4, result.length);
        assertAnyTrue(result, r -> "resource a".equals(r.getName()) && "urn:article:a".equals(r.getContentUri().toString()));
        assertAnyTrue(result, r -> "resource aa".equals(r.getName()) && "urn:article:aa".equals(r.getContentUri().toString()));
        assertAnyTrue(result, r -> "resource aaa".equals(r.getName()) && "urn:article:aaa".equals(r.getContentUri().toString()));
        assertAnyTrue(result, r -> "resource aab".equals(r.getName()) && "urn:article:aab".equals(r.getContentUri().toString()));
        assertAllTrue(result, r -> !r.getPaths().isEmpty());
        assertAllTrue(result, ResourceWithTopicConnectionDTO::isPrimary);
    }

    @Test
    public void resources_by_topic_id_recursively_are_ordered_by_rank_in_parent() throws Exception {
        testSeeder.resourcesBySubjectIdTestSetup();
        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:5/resources?recursive=true");
        final var result = testUtils.getObject(ResourceWithTopicConnectionDTO[].class, response);
        assertEquals(6, result.length);
        assertEquals("urn:resource:3", result[0].getId().toString());
        assertEquals("urn:resource:5", result[1].getId().toString());
        assertEquals("urn:resource:4", result[2].getId().toString());
        assertEquals("urn:resource:6", result[3].getId().toString());
        assertEquals("urn:resource:7", result[4].getId().toString());
        assertEquals("urn:resource:8", result[5].getId().toString());

    }

    @Test
    public void primary_status_is_returned_on_resources() throws Exception {
        final var resource = builder.resource("r1", rb -> {
            rb.name("resource 1");
        });

        builder.node(tb -> {
            tb.nodeType(NodeType.TOPIC);
            tb.name("topic 1");
            tb.publicId("urn:topic:rt:1201");
            tb.resource(resource, true);
        });

        builder.node(tb -> {
            tb.nodeType(NodeType.TOPIC);
            tb.name("topic 2");
            tb.publicId("urn:topic:rt:1202");
            tb.resource(resource, false);
        });

        {
            MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:rt:1201/resources");
            final var result = testUtils.getObject(ResourceWithTopicConnectionDTO[].class, response);
            assertEquals(1, result.length);
            assertTrue(result[0].isPrimary());
        }
        {
            MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:rt:1202/resources");
            final var result = testUtils.getObject(ResourceWithTopicConnectionDTO[].class, response);
            assertEquals(1, result.length);
            assertFalse(result[0].isPrimary());
        }
    }

    @Test
    public void can_get_urls_for_resources_for_a_topic_recursively() throws Exception {
        builder.node(s -> s.publicId("urn:subject:1")
                .nodeType(NodeType.SUBJECT)
                .isContext(true)
                .child(t -> t
                        .nodeType(NodeType.TOPIC)
                        .publicId("urn:topic:a")
                        .resource(true, r -> r.publicId("urn:resource:a"))
                        .child(st -> st
                                .nodeType(NodeType.TOPIC)
                                .publicId("urn:topic:aa")
                                .resource(true, r -> r.publicId("urn:resource:aa"))
                                .child(st2 -> st2
                                        .nodeType(NodeType.TOPIC)
                                        .publicId("urn:topic:aaa")
                                        .resource("aaa", true, r -> r.publicId("urn:resource:aaa"))
                                )
                                .child(st2 -> st2
                                        .nodeType(NodeType.TOPIC)
                                        .publicId("urn:topic:aab")
                                        .resource(true, r -> r.publicId("urn:resource:aab"))
                                )
                        )
                ));

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:a/resources?recursive=true");
        final var result = testUtils.getObject(ResourceWithTopicConnectionDTO[].class, response);

        assertEquals(4, result.length);
        assertAnyTrue(result, r -> "/subject:1/topic:a/resource:a".equals(r.getPath()));
        assertAnyTrue(result, r -> "/subject:1/topic:a/topic:aa/resource:aa".equals(r.getPath()));
        assertAnyTrue(result, r -> "/subject:1/topic:a/topic:aa/topic:aaa/resource:aaa".equals(r.getPath()));
        assertAnyTrue(result, r -> "/subject:1/topic:a/topic:aa/topic:aab/resource:aab".equals(r.getPath()));
        assertAllTrue(result, ResourceWithTopicConnectionDTO::isPrimary);
    }

    @Test
    public void can_get_resources_for_a_topic_without_child_topic_resources() throws Exception {
        builder.node(s -> s
                .nodeType(NodeType.SUBJECT)
                .isContext(true)
                .child(t -> t
                        .nodeType(NodeType.TOPIC)
                        .name("a")
                        .publicId("urn:topic:1")
                        .child(st -> st.nodeType(NodeType.TOPIC).name("subtopic").resource(r -> r.name("subtopic resource")))
                        .resource(r -> r.name("resource 1"))
                        .resource(r -> r.name("resource 2"))
                ));

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:1/resources");
        final var result = testUtils.getObject(ResourceWithTopicConnectionDTO[].class, response);

        assertEquals(2, result.length);
        assertAnyTrue(result, r -> "resource 1".equals(r.getName()));
        assertAnyTrue(result, r -> "resource 2".equals(r.getName()));
    }

    //@Test TODO - relevance filtering is broken after move from filter
    public void resources_can_be_filtered_by_relevance() throws Exception {
        testSeeder.resourceWithFiltersAndRelevancesTestSetup();

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:1/resources?relevance=urn:relevance:core");
        final var resources = testUtils.getObject(ResourceWithTopicConnectionDTO[].class, response);
        assertEquals(10, resources.length);

        MockHttpServletResponse response2 = testUtils.getResource("/v1/topics/urn:topic:1/resources?relevance=urn:relevance:supplementary");
        final var resources2 = testUtils.getObject(ResourceWithTopicConnectionDTO[].class, response2);
        assertEquals(5, resources2.length);

    }

    @Test
    public void resources_without_filters_can_be_filtered_by_relevance() throws Exception {
        testSeeder.resourceWithRelevancesButWithoutFiltersTestSetup();

        {
            MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:1:1/resources?relevance=urn:relevance:core");
            final var resources = testUtils.getObject(ResourceWithTopicConnectionDTO[].class, response);
            assertEquals(5, resources.length);
        }
        {
            MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:2:1/resources?relevance=urn:relevance:core");
            final var resources = testUtils.getObject(ResourceWithTopicConnectionDTO[].class, response);
            assertEquals(5, resources.length);
        }

        {
            MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:1:1/resources?relevance=urn:relevance:supplementary");
            final var resources = testUtils.getObject(ResourceWithTopicConnectionDTO[].class, response);
            assertEquals(0, resources.length);
        }
        {
            MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:2:1/resources?relevance=urn:relevance:supplementary");
            final var resources = testUtils.getObject(ResourceWithTopicConnectionDTO[].class, response);
            assertEquals(5, resources.length);
        }

    }

    @Test
    public void resources_without_filters_can_be_filtered_by_relevance_and_core_is_default() throws Exception {
        testSeeder.resourceWithRelevancesAndOneNullRelevanceButWithoutFiltersTestSetup();

        {
            MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:1:1/resources?relevance=urn:relevance:core");
            final var resources = testUtils.getObject(ResourceWithTopicConnectionDTO[].class, response);
            assertEquals(5, resources.length);
        }
        {
            MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:2:1/resources?relevance=urn:relevance:core");
            final var resources = testUtils.getObject(ResourceWithTopicConnectionDTO[].class, response);
            assertEquals(5, resources.length);
        }

        {
            MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:1:1/resources?relevance=urn:relevance:supplementary");
            final var resources = testUtils.getObject(ResourceWithTopicConnectionDTO[].class, response);
            assertEquals(0, resources.length);
        }
        {
            MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:2:1/resources?relevance=urn:relevance:supplementary");
            final var resources = testUtils.getObject(ResourceWithTopicConnectionDTO[].class, response);
            assertEquals(5, resources.length);
        }

    }

    @Test
    public void resources_can_be_filtered_by_filters() throws Exception {
        testSeeder.resourceWithFiltersAndRelevancesTestSetup();

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:1/resources?filter=urn:filter:1");
        final var resources = testUtils.getObject(ResourceWithTopicConnectionDTO[].class, response);
        // Filters are removed
        assertEquals(10, resources.length);

        MockHttpServletResponse response2 = testUtils.getResource("/v1/topics/urn:topic:1/resources?filter=urn:filter:1,urn:filter:2");
        final var resources2 = testUtils.getObject(ResourceWithTopicConnectionDTO[].class, response2);
        // Filters are removed
        assertEquals(10, resources2.length);
    }

    @Test
    public void resources_are_ordered_relative_to_parent() throws Exception {
        testSeeder.resourcesBySubjectNodeIdTestSetup();

        MockHttpServletResponse response = testUtils.getResource("/v1/subjects/urn:subject:1/resources");
        final var resources = testUtils.getObject(ResourceDTO[].class, response);

        assertEquals(10, resources.length);
        assertEquals("R:9", resources[0].getName());
        assertEquals("R:1", resources[1].getName());
        assertEquals("R:2", resources[2].getName());
        assertEquals("R:10", resources[3].getName());
        assertEquals("R:3", resources[4].getName());
        assertEquals("R:5", resources[5].getName());
        assertEquals("R:4", resources[6].getName());
        assertEquals("R:6", resources[7].getName());
        assertEquals("R:7", resources[8].getName());
        assertEquals("R:8", resources[9].getName());
    }

    @Test
    public void resources_can_have_content_uri() throws Exception {
        URI id = builder.node(n -> n
                .nodeType(NodeType.SUBJECT)
                .isContext(true)
                .publicId("urn:subject:1")
                .child(c -> c
                        .nodeType(NodeType.TOPIC)
                        .publicId("urn:topic:1:1")
                        .resource(r -> r
                                .publicId("urn:resource:1")
                                .contentUri("urn:article:1")
                        )
                )
        ).getPublicId();

        MockHttpServletResponse response = testUtils.getResource("/v1/subjects/" + id + "/resources");
        final var resources = testUtils.getObject(ResourceDTO[].class, response);

        assertEquals("urn:article:1", resources[0].getContentUri().toString());
        //assertEquals("/subject:1/topic:1:1/resource:1", resources[0].getPath());
    }

    @Test
    public void can_get_resources_for_a_subject_and_its_topics_recursively_with_metadata() throws Exception {
        URI id = builder.node(NodeType.SUBJECT, n -> n
                .isContext(true)
                .publicId("urn:subject:1")
                .name("Subject")
                .child("topic a", NodeType.TOPIC, t -> t
                        .name("topic a")
                        .publicId("urn:topic:1")
                        .resource(r -> r.name("resource a").resourceType(rt -> rt.name("assignment"))))
                .child("topic b", NodeType.TOPIC, t -> t
                        .name("topic b")
                        .publicId("urn:topic:2")
                        .resource(r -> r.name("resource b").resourceType(rt -> rt.name("lecture")))
                        .child("subtopic", NodeType.TOPIC, s -> s
                                .name("subtopic")
                                .publicId("urn:topic:3")
                                .resource(r -> r.name("sub resource")))
                )
        ).getPublicId();

        MockHttpServletResponse response = testUtils.getResource("/v1/subjects/" + id + "/resources");
        final var resources = testUtils.getObject(ResourceWithNodeConnectionDTO[].class, response);

        assertEquals(3, resources.length);

        assertAnyTrue(resources, r -> r.getConnectionId().equals(first(builder.node("topic a").getNodeResources()).getPublicId()));
        assertAnyTrue(resources, r -> r.getConnectionId().equals(first(builder.node("topic b").getNodeResources()).getPublicId()));
        assertAnyTrue(resources, r -> r.getConnectionId().equals(first(builder.node("subtopic").getNodeResources()).getPublicId()));

        assertAllTrue(resources, r -> r.getMetadata() != null);
        assertAllTrue(resources, r -> r.getMetadata().isVisible());
        assertAllTrue(resources, r -> r.getMetadata().getGrepCodes().size() == 1);
    }

    @Test
    public void can_get_urls_for_all_resources() throws Exception {
        builder.node(n -> n
                .nodeType(NodeType.SUBJECT)
                .isContext(true)
                .publicId("urn:subject:1")
                .child(c -> c
                        .nodeType(NodeType.TOPIC)
                        .publicId("urn:topic:1")
                        .resource(true, r -> r
                                .name("resource 1")
                                .publicId("urn:resource:1")
                                .resourceType(rt -> rt.name("lecture")))
                )
                .child(c -> c
                        .nodeType(NodeType.TOPIC)
                        .publicId("urn:topic:2")
                        .resource(true, r -> r
                                .name("resource 2")
                                .publicId("urn:resource:2")
                                .resourceType(rt -> rt.name("lecture")))
                        .child(c1 -> c1
                                .nodeType(NodeType.TOPIC)
                                .publicId("urn:topic:3")
                                .resource(true, r -> r
                                        .name("resouce 3")
                                        .publicId("urn:resource:3")
                                        .resourceType(rt -> rt.name("lecture")))
                        )
                )
        );

        MockHttpServletResponse response = testUtils.getResource("/v1/subjects/urn:subject:1/resources");
        final var resources = testUtils.getObject(ResourceDTO[].class, response);

        assertEquals(3, resources.length);
        assertAnyTrue(resources, r -> r.getPath().equals("/subject:1/topic:1/resource:1"));
        assertAnyTrue(resources, r -> r.getPath().equals("/subject:1/topic:2/resource:2"));
        assertAnyTrue(resources, r -> r.getPath().equals("/subject:1/topic:2/topic:3/resource:3"));
    }

}
