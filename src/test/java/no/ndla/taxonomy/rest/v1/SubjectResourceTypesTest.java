package no.ndla.taxonomy.rest.v1;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;

import static no.ndla.taxonomy.TestUtils.*;
import static org.junit.Assert.assertEquals;

public class SubjectResourceTypesTest extends RestTest {

    @Test
    public void can_have_several_resource_types_recursively() throws Exception {
        URI subjectId = builder.subject(s -> s
                .name("subject")
                .topic(t -> t
                        .name("topic")
                        .subtopic(st -> st
                                .resource(r -> r
                                        .name("resource 1")
                                        .resourceType(rt -> rt.name("lecture"))
                                        .resourceType(rt -> rt.name("assignment"))
                                )
                        )
                        .resource(r -> r
                                .name("resource 2")
                                .resourceType(rt -> rt.name("lecture"))
                                .resourceType(rt -> rt.name("assignment"))
                        )
                )
        ).getPublicId();

        MockHttpServletResponse response = getResource("/v1/subjects/" + subjectId + "/resources");
        Topics.ResourceIndexDocument[] result = getObject(Topics.ResourceIndexDocument[].class, response);

        assertEquals(2, result.length);
        assertEquals(2, result[0].resourceTypes.size());
        assertEquals(2, result[1].resourceTypes.size());
    }

    @Test
    public void can_have_no_resource_type() throws Exception {
        URI subjectId = builder.subject(s -> s
                .name("subject")
                .topic(t -> t
                        .name("topic")
                        .subtopic(st -> st
                                .resource(r -> r
                                        .name("resource 1")
                                )
                        )
                )
        ).getPublicId();

        MockHttpServletResponse response = getResource("/v1/subjects/" + subjectId + "/resources");
        Topics.ResourceIndexDocument[] result = getObject(Topics.ResourceIndexDocument[].class, response);

        assertEquals(1, result.length);
        assertEquals(0, result[0].resourceTypes.size());
    }

    @Test
    public void can_get_resources_for_a_subject_filtered_on_resource_type() throws Exception {
        builder.resourceType("assignment").getPublicId();
        URI lecture = builder.resourceType("lecture").getPublicId();

        URI subjectId = builder.subject(s -> s
                .name("subject")
                .topic(t -> t
                        .name("a")
                        .subtopic(sub -> sub.name("subtopic").resource(r -> r.name("a lecture in a subtopic").resourceType("lecture")))
                        .resource(r -> r.name("an assignment").resourceType("assignment"))
                        .resource(r -> r.name("a lecture").resourceType("lecture"))
                )
        ).getPublicId();

        MockHttpServletResponse response = getResource("/v1/subjects/" + subjectId + "/resources?type=" + lecture);
        Topics.ResourceIndexDocument[] result = getObject(Topics.ResourceIndexDocument[].class, response);

        assertEquals(2, result.length);
        assertAnyTrue(result, r -> "a lecture".equals(r.name));
    }
}
