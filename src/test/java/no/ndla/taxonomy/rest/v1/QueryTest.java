/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.service.dtos.EntityWithPathDTO;
import no.ndla.taxonomy.service.dtos.NodeDTO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.swing.text.html.parser.Entity;

import static no.ndla.taxonomy.TestUtils.assertAnyTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class QueryTest extends RestTest {

    @Test
    public void can_get_resource_by_contentURI() throws Exception {
        builder.node(NodeType.RESOURCE, r -> r.publicId("urn:resource:1").contentUri("urn:article:345")
                .resourceType(rt -> rt.name("Subject material")));

        MockHttpServletResponse response = testUtils.getResource("/v1/queries/resources?contentURI=urn:article:345");
        var resources = testUtils.getObject(NodeDTO[].class, response);

        assertEquals(1, resources.length);
        assertEquals(resources[0].getResourceTypes().size(), 1);
    }

    @Test
    public void no_resources_matching_contentURI() throws Exception {
        MockHttpServletResponse response = testUtils.getResource("/v1/queries/resources?contentURI=urn:article:345");
        var resources = testUtils.getObject(NodeDTO[].class, response);

        assertEquals(0, resources.length);
    }

    @Test
    public void can_get_all_resources_matching_contentURI() throws Exception {
        builder.node(NodeType.RESOURCE, r -> r.publicId("urn:resource:1").contentUri("urn:article:345")
                .resourceType(rt -> rt.name("Subject material")));

        builder.node(NodeType.RESOURCE, r -> r.publicId("urn:resource:2").contentUri("urn:article:3"));

        MockHttpServletResponse response = testUtils.getResource("/v1/queries/resources?contentURI=urn:article:345");
        var resources = testUtils.getObject(NodeDTO[].class, response);

        assertEquals(1, resources.length);
        assertAnyTrue(resources, r -> "urn:resource:1".equals(r.getId().toString()));
    }

    @Test
    public void can_get_all_resource_types_for_a_resource() throws Exception {
        builder.node(NodeType.RESOURCE, r -> r.publicId("urn:resource:1").contentUri("urn:article:345")
                .resourceType(rt -> rt.name("Subject material")).resourceType(rt -> rt.name("Learning path")));

        MockHttpServletResponse response = testUtils.getResource("/v1/queries/resources?contentURI=urn:article:345");
        var resources = testUtils.getObject(NodeDTO[].class, response);

        assertEquals(1, resources.length);
        assertEquals(2, resources[0].getResourceTypes().size());
    }

    @Test
    public void can_get_translated_name_for_resource() throws Exception {
        builder.node(NodeType.RESOURCE,
                r -> r.publicId("urn:resource:1").name("Resource").translation("nb", tr -> tr.name("ressurs"))
                        .contentUri("urn:article:345").resourceType(rt -> rt.name("Subject material")));

        MockHttpServletResponse response = testUtils
                .getResource("/v1/queries/resources?contentURI=urn:article:345&language=nb");
        var resources = testUtils.getObject(NodeDTO[].class, response);

        assertEquals(1, resources.length);
        assertEquals(resources[0].getResourceTypes().size(), 1);
        assertEquals("ressurs", resources[0].getName());
    }

    @Test
    public void can_get_a_topic_matching_contentURI() throws Exception {
        builder.node(NodeType.RESOURCE, r -> r.publicId("urn:resource:1").contentUri("urn:article:345")
                .resourceType(rt -> rt.name("Subject material")));

        builder.node(NodeType.TOPIC, r -> r.publicId("urn:topic:2").contentUri("urn:article:345"));

        MockHttpServletResponse response = testUtils.getResource("/v1/queries/topics?contentURI=urn:article:345");
        var resources = testUtils.getObject(NodeDTO[].class, response);

        assertEquals(1, resources.length);
        assertAnyTrue(resources, r -> "urn:topic:2".equals(r.getId().toString()));
    }

    @Test
    public void can_get_all_topics_matching_contentURI() throws Exception {
        builder.node(NodeType.TOPIC, r -> r.publicId("urn:topic:2").contentUri("urn:article:345"));

        builder.node(NodeType.TOPIC, r -> r.publicId("urn:topic:3").contentUri("urn:article:345"));

        MockHttpServletResponse response = testUtils.getResource("/v1/queries/topics?contentURI=urn:article:345");
        var resources = testUtils.getObject(NodeDTO[].class, response);

        assertEquals(2, resources.length);
        assertAnyTrue(resources, r -> "urn:topic:2".equals(r.getId().toString()));
        assertAnyTrue(resources, r -> "urn:topic:3".equals(r.getId().toString()));
    }

    @Test
    public void no_topics_matching_contentURI() throws Exception {
        MockHttpServletResponse response = testUtils.getResource("/v1/queries/topics?contentURI=urn:article:345");
        NodeDTO[] resources = testUtils.getObject(NodeDTO[].class, response);

        assertEquals(0, resources.length);
    }

    @Test
    public void can_get_translated_name_for_topic() throws Exception {
        builder.node(NodeType.TOPIC, r -> r.publicId("urn:topic:2").name("topic")
                .translation("nb", tr -> tr.name("Emne")).contentUri("urn:article:345"));

        MockHttpServletResponse response = testUtils
                .getResource("/v1/queries/topics?contentURI=urn:article:345&language=nb");
        var resources = testUtils.getObject(NodeDTO[].class, response);

        assertEquals(1, resources.length);
        assertEquals("Emne", resources[0].getName());
        assertAnyTrue(resources, r -> "urn:topic:2".equals(r.getId().toString()));
    }
}
