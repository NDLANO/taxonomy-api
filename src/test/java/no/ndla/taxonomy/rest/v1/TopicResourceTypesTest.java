package no.ndla.taxonomy.rest.v1;

import no.ndla.taxonomy.domain.ResourceType;
import no.ndla.taxonomy.rest.v1.dtos.topics.ResourceIndexDocument;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;

import static no.ndla.taxonomy.TestUtils.assertAnyTrue;
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

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:1/resources?type=" + assignment + "," + lecture);
        ResourceIndexDocument[] result = testUtils.getObject(ResourceIndexDocument[].class, response);

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

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:1/resources");
        ResourceIndexDocument[] result = testUtils.getObject(ResourceIndexDocument[].class, response);

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

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/" + topic + "/resources?recursive=true");
        ResourceIndexDocument[] result = testUtils.getObject(ResourceIndexDocument[].class, response);

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

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/" + topic + "/resources?recursive=true");
        ResourceIndexDocument[] result = testUtils.getObject(ResourceIndexDocument[].class, response);

        assertEquals(1, result.length);
        assertEquals(0, result[0].resourceTypes.size());
    }
}
