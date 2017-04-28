package no.ndla.taxonomy.service.rest.v1;

import no.ndla.taxonomy.service.domain.ResourceType;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;

import static no.ndla.taxonomy.service.TestUtils.assertAnyTrue;
import static no.ndla.taxonomy.service.TestUtils.getObject;
import static no.ndla.taxonomy.service.TestUtils.getResource;
import static org.junit.Assert.assertEquals;

public class TopicResourceTypesTest extends RestTest {

    @Test
    public void can_get_resources_for_a_topic_filtered_on_resource_type() throws Exception {
        URI assignment = builder.resourceType("assignment").getPublicId();
        URI lecture = builder.resourceType("lecture").getPublicId();

        builder.subject(s -> s.topic(t -> t
                .name("a")
                .publicId("urn:topic:1")
                .subtopic(sub -> sub.name("subtopic").resource(r -> r.name("a lecture in a subtopic").resourceType("lecture")))
                .resource(r -> r.name("an assignment").resourceType("assignment"))
                .resource(r -> r.name("a lecture").resourceType("lecture"))
        ));

        MockHttpServletResponse response = getResource("/v1/topics/urn:topic:1/resources?type=" + assignment + "," + lecture);
        Topics.ResourceIndexDocument[] result = getObject(Topics.ResourceIndexDocument[].class, response);

        assertEquals(2, result.length);
        assertAnyTrue(result, r -> "a lecture".equals(r.name));
        assertAnyTrue(result, r -> "an assignment".equals(r.name));
    }


    @Test
    public void can_have_several_resource_types() throws Exception {
        ResourceType lecture = builder.resourceType("lecture", rt -> rt.name("lecture"));
        ResourceType assignment = builder.resourceType("assignment", rt -> rt.name("assignment"));

        builder.subject(s -> s.topic(t -> t
                .name("topic")
                .publicId("urn:topic:1")
                .resource(r -> r
                        .name("resource")
                        .resourceType(lecture)
                        .resourceType(assignment)
                )
        ));

        MockHttpServletResponse response = getResource("/v1/topics/urn:topic:1/resources");
        Topics.ResourceIndexDocument[] result = getObject(Topics.ResourceIndexDocument[].class, response);

        assertEquals(1, result.length);
        assertEquals(2, result[0].resourceTypes.size());
        assertAnyTrue(result[0].resourceTypes, rt -> rt.id.equals(lecture.getPublicId()));
        assertAnyTrue(result[0].resourceTypes, rt -> rt.id.equals(assignment.getPublicId()));
    }

    @Test
    public void can_have_several_resource_types_recursively() throws Exception {
        ResourceType lecture = builder.resourceType("lecture", rt -> rt.name("lecture"));
        ResourceType assignment = builder.resourceType("assignment", rt -> rt.name("assignment"));

        URI topic = builder.topic(t -> t
                .name("topic")
                .subtopic(st -> st
                        .resource(r -> r
                                .name("resource 1")
                                .resourceType(lecture)
                                .resourceType(assignment)
                        )
                )
                .resource(r -> r
                        .name("resource 2")
                        .resourceType(lecture)
                        .resourceType(assignment)
                )
        ).getPublicId();

        MockHttpServletResponse response = getResource("/v1/topics/" + topic + "/resources?recursive=true");
        Topics.ResourceIndexDocument[] result = getObject(Topics.ResourceIndexDocument[].class, response);

        assertEquals(2, result.length);
        assertEquals(2, result[0].resourceTypes.size());
        assertEquals(2, result[1].resourceTypes.size());
        assertAnyTrue(result[0].resourceTypes, rt -> rt.id.equals(lecture.getPublicId()));
        assertAnyTrue(result[0].resourceTypes, rt -> rt.id.equals(assignment.getPublicId()));
        assertAnyTrue(result[1].resourceTypes, rt -> rt.id.equals(lecture.getPublicId()));
        assertAnyTrue(result[1].resourceTypes, rt -> rt.id.equals(assignment.getPublicId()));
    }

    @Test
    public void can_have_no_resource_type() throws Exception {
        URI topic = builder.topic(t -> t
                .name("topic")
                .subtopic(st -> st
                        .resource(r -> r
                                .name("resource 1")
                        )
                )
        ).getPublicId();

        MockHttpServletResponse response = getResource("/v1/topics/" + topic + "/resources?recursive=true");
        Topics.ResourceIndexDocument[] result = getObject(Topics.ResourceIndexDocument[].class, response);

        assertEquals(1, result.length);
        assertEquals(0, result[0].resourceTypes.size());
    }
}
