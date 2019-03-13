package no.ndla.taxonomy.rest.v1.controllers;

import no.ndla.taxonomy.rest.v1.dtos.queries.ResourceIndexDocument;
import no.ndla.taxonomy.rest.v1.dtos.queries.TopicIndexDocument;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.mock.web.MockHttpServletResponse;

import static no.ndla.taxonomy.TestUtils.*;
import static org.junit.Assert.assertEquals;

public class QueryTest extends RestTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void can_get_resource_by_contentURI() throws Exception {
        builder.resource(r -> r
                .publicId("urn:resource:1")
                .contentUri("urn:article:345")
                .resourceType(rt -> rt.name("Subject material"))
        );

        MockHttpServletResponse response = getResource("/v1/queries/resources?contentURI=urn:article:345");
        ResourceIndexDocument[] resources = getObject(ResourceIndexDocument[].class, response);

        assertEquals(1, resources.length);
        assertEquals(resources[0].resourceTypes.size(), 1);
    }

    @Test
    public void no_resources_matching_contentURI() throws Exception {
        MockHttpServletResponse response = getResource("/v1/queries/resources?contentURI=urn:article:345");
        ResourceIndexDocument[] resources = getObject(ResourceIndexDocument[].class, response);

        assertEquals(0, resources.length);
    }

    @Test
    public void can_get_all_resources_matching_contentURI() throws Exception {
        builder.resource(r -> r
                .publicId("urn:resource:1")
                .contentUri("urn:article:345")
                .resourceType(rt -> rt.name("Subject material"))
        );

        builder.resource(r -> r
                .publicId("urn:resource:2")
                .contentUri("urn:article:3"));

        builder.resource(r -> r
                .publicId("urn:resource:3")
                .contentUri("urn:article:345")
                .resourceType(rt -> rt.name("Subject material"))
        );

        MockHttpServletResponse response = getResource("/v1/queries/resources?contentURI=urn:article:345");
        ResourceIndexDocument[] resources = getObject(ResourceIndexDocument[].class, response);

        assertEquals(2, resources.length);
        assertAnyTrue(resources, r -> "urn:resource:1".equals(r.id.toString()));
        assertAnyTrue(resources, r -> "urn:resource:3".equals(r.id.toString()));
    }

    @Test
    public void can_get_all_resource_types_for_a_resource() throws Exception {
        builder.resource(r -> r
                .publicId("urn:resource:1")
                .contentUri("urn:article:345")
                .resourceType(rt -> rt.name("Subject material"))
                .resourceType(rt -> rt.name("Learning path"))
        );

        MockHttpServletResponse response = getResource("/v1/queries/resources?contentURI=urn:article:345");
        ResourceIndexDocument[] resources = getObject(ResourceIndexDocument[].class, response);

        assertEquals(1, resources.length);
        assertEquals(2, resources[0].resourceTypes.size());
    }

    @Test
    public void can_get_translated_name_for_resource() throws Exception {
        builder.resource(r -> r
                .publicId("urn:resource:1")
                .name("Resource")
                .translation("nb", tr -> tr.name("ressurs"))
                .contentUri("urn:article:345")
                .resourceType(rt -> rt.name("Subject material"))
        );

        MockHttpServletResponse response = getResource("/v1/queries/resources?contentURI=urn:article:345&language=nb");
        ResourceIndexDocument[] resources = getObject(ResourceIndexDocument[].class, response);

        assertEquals(1, resources.length);
        assertEquals(resources[0].resourceTypes.size(), 1);
        assertEquals("ressurs", resources[0].name);
    }

    @Test
    public void can_get_a_topic_matching_contentURI() throws Exception {
        builder.resource(r -> r
                .publicId("urn:resource:1")
                .contentUri("urn:article:345")
                .resourceType(rt -> rt.name("Subject material"))
        );

        builder.topic(r -> r
                .publicId("urn:topic:2")
                .contentUri("urn:article:345"));

        MockHttpServletResponse response = getResource("/v1/queries/topics?contentURI=urn:article:345");
        ResourceIndexDocument[] resources = getObject(ResourceIndexDocument[].class, response);

        assertEquals(1, resources.length);
        assertAnyTrue(resources, r -> "urn:topic:2".equals(r.id.toString()));
    }

    @Test
    public void can_get_all_topics_matching_contentURI() throws Exception {
        builder.topic(r -> r
                .publicId("urn:topic:2")
                .contentUri("urn:article:345"));

        builder.topic(r -> r
                .publicId("urn:topic:3")
                .contentUri("urn:article:345"));

        MockHttpServletResponse response = getResource("/v1/queries/topics?contentURI=urn:article:345");
        ResourceIndexDocument[] resources = getObject(ResourceIndexDocument[].class, response);

        assertEquals(2, resources.length);
        assertAnyTrue(resources, r -> "urn:topic:2".equals(r.id.toString()));
        assertAnyTrue(resources, r -> "urn:topic:3".equals(r.id.toString()));
    }

    @Test
    public void no_topics_matching_contentURI() throws Exception {
        MockHttpServletResponse response = getResource("/v1/queries/topics?contentURI=urn:article:345");
        TopicIndexDocument[] resources = getObject(TopicIndexDocument[].class, response);

        assertEquals(0, resources.length);
    }

    @Test
    public void can_get_translated_name_for_topic() throws Exception {
        builder.topic(r -> r
                .publicId("urn:topic:2")
                .name("topic")
                .translation("nb", tr -> tr.name("Emne"))
                .contentUri("urn:article:345"));

        MockHttpServletResponse response = getResource("/v1/queries/topics?contentURI=urn:article:345&language=nb");
        ResourceIndexDocument[] resources = getObject(ResourceIndexDocument[].class, response);

        assertEquals(1, resources.length);
        assertEquals("Emne", resources[0].name);
        assertAnyTrue(resources, r -> "urn:topic:2".equals(r.id.toString()));
    }
}

