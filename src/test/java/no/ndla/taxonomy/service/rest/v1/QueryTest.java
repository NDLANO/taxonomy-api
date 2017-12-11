package no.ndla.taxonomy.service.rest.v1;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static no.ndla.taxonomy.service.TestUtils.assertAnyTrue;
import static no.ndla.taxonomy.service.TestUtils.getObject;
import static no.ndla.taxonomy.service.TestUtils.getResource;
import static org.junit.Assert.assertEquals;

public class QueryTest extends RestTest {

    @Test
    public void can_get_resource_by_contentURI() throws Exception {
        builder.resource(r -> r
                .publicId("urn:resource:1")
                .contentUri("urn:article:345")
                .resourceType(rt -> rt.name("Subject material"))
        );

        MockHttpServletResponse response = getResource("/v1/queries/query?contentURI=urn:article:345");
        Queries.ResourceIndexDocument[] resources = getObject(Queries.ResourceIndexDocument[].class, response);

        assertEquals(1, resources.length);
        assertEquals(resources[0].resourceTypes.size(),1);
    }

    @Test
    public void no_resources_matching_contentURI() throws Exception{
        MockHttpServletResponse response = getResource("/v1/queries/query?contentURI=urn:article:345&entityType=resource");
        Queries.ResourceIndexDocument[] resources = getObject(Queries.ResourceIndexDocument[].class, response);

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

        MockHttpServletResponse response = getResource("/v1/queries/query?contentURI=urn:article:345&entityType=resource");
        Queries.ResourceIndexDocument[] resources = getObject(Queries.ResourceIndexDocument[].class, response);

        assertEquals(2, resources.length);
        assertAnyTrue(resources, r -> "urn:resource:1".equals(r.id.toString()));
        assertAnyTrue(resources, r -> "urn:resource:3".equals(r.id.toString()));
    }

    @Test
    public void can_filter_on_entity_type() {

    }
}
