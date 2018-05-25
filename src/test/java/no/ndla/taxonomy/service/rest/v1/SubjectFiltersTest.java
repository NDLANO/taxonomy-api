package no.ndla.taxonomy.service.rest.v1;

import no.ndla.taxonomy.service.domain.*;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;

import static no.ndla.taxonomy.service.TestUtils.*;
import static org.junit.Assert.assertEquals;

public class SubjectFiltersTest extends RestTest {
    private Filter vg1, vg2;
    private Relevance core, supplementary;

    @Before
    public void before() throws Exception {
        core = builder.relevance(r -> r.publicId("urn:relevance:core").name("Core material"));
        supplementary = builder.relevance(r -> r.publicId("urn:relevance:supplementary").name("Supplementary material"));
        vg1 = builder.filter(f -> f.name("VG1").publicId("urn:filter:vg1"));
        vg2 = builder.filter(f -> f.name("VG2").publicId("urn:filter:vg2"));
    }

    @Test
    public void can_get_filters_for_subject() throws Exception {
        builder.subject(s -> s
                .name("Byggfag")
                .publicId("urn:subject:1")
                .filter(f -> f.name("Tømrer"))
                .filter(f -> f.name("Rørlegger"))
        );

        MockHttpServletResponse response = getResource("/v1/subjects/urn:subject:1/filters");
        Subjects.FilterIndexDocument[] filters = getObject(Subjects.FilterIndexDocument[].class, response);

        assertEquals(2, filters.length);
        assertAnyTrue(filters, f -> f.name.equals("Tømrer"));
        assertAnyTrue(filters, f -> f.name.equals("Rørlegger"));
    }

    @Test
    public void can_get_resources_belonging_to_a_filter_for_a_subject() throws Exception {
        URI subjectId = builder.subject(s -> s
                .name("subject")
                .topic(t -> t
                        .name("a")
                        .subtopic(sub -> sub.name("subtopic").resource(r -> r.name("a lecture in a subtopic").filter(vg1, core)))
                        .resource(r -> r.name("an assignment").filter(vg1, core))
                        .resource(r -> r.name("a lecture").filter(vg2, core))
                )
        ).getPublicId();

        MockHttpServletResponse response = getResource("/v1/subjects/" + subjectId + "/resources?filter=" + vg1.getPublicId());
        Topics.ResourceIndexDocument[] result = getObject(Topics.ResourceIndexDocument[].class, response);

        assertEquals(2, result.length);
        assertAnyTrue(result, r -> "a lecture in a subtopic".equals(r.name));
        assertAnyTrue(result, r -> "an assignment".equals(r.name));
    }

    @Test
    public void can_get_resources_belonging_to_a_filter_and_resource_type_for_a_subject() throws Exception {
        ResourceType type = builder.resourceType(rt -> rt.name("Video").publicId("urn:resourcetype:video"));

        URI subjectId = builder.subject(s -> s
                .name("subject")
                .topic(t -> t
                        .name("a")
                        .subtopic(sub -> sub.name("subtopic").resource(r -> r.name("a lecture in a subtopic").filter(vg1, core).resourceType(type)))
                        .resource(r -> r.name("an assignment").filter(vg1, core))
                        .resource(r -> r.name("a lecture").filter(vg2, core))
                )
        ).getPublicId();

        MockHttpServletResponse response = getResource("/v1/subjects/" + subjectId + "/resources?filter=" + vg1.getPublicId() + "&type=" + type.getPublicId());
        Topics.ResourceIndexDocument[] result = getObject(Topics.ResourceIndexDocument[].class, response);

        assertEquals(1, result.length);
        assertAnyTrue(result, r -> "a lecture in a subtopic".equals(r.name));
    }

    @Test
    public void can_get_topics_with_filter() throws Exception {
        Subject subject = builder.subject(s -> s
                .name("physics")
                .topic(t -> t
                        .name("statics")
                        .filter(vg1, core)
                )
                .topic(t -> t.name("electricity").filter(vg2, core))
                .topic(t -> t.name("optics").filter(vg1, core))
        );

        MockHttpServletResponse response = getResource("/v1/subjects/" + subject.getPublicId() + "/topics?filter=" + vg1.getPublicId());
        Subjects.SubTopicIndexDocument[] topics = getObject(Subjects.SubTopicIndexDocument[].class, response);

        assertEquals(2, topics.length);
        assertAnyTrue(topics, t -> "statics".equals(t.name));
        assertAnyTrue(topics, t -> "optics".equals(t.name));
        assertAnyTrue(topics[0].filters, f -> vg1.getPublicId().equals(f.id));
        assertAnyTrue(topics[0].filters, f -> vg1.getName().equals(f.name));
        assertAnyTrue(topics[0].filters, f -> core.getPublicId().equals(f.relevanceId));
        assertEquals(1, topics[0].filters.size());
    }

    @Test
    public void topic_can_have_multiple_filters() throws Exception {
        Subject subject = builder.subject(s -> s
                .name("physics")
                .topic(t -> t
                        .name("statics")
                        .filter(vg1, core)
                        .filter(vg2, supplementary))
        );

        MockHttpServletResponse response = getResource("/v1/subjects/" + subject.getPublicId() + "/topics");
        Subjects.SubTopicIndexDocument[] topics = getObject(Subjects.SubTopicIndexDocument[].class, response);

        assertEquals(2, topics[0].filters.size());
        assertAnyTrue(topics[0].filters, f -> vg1.getPublicId().equals(f.id));
        assertAnyTrue(topics[0].filters, f -> core.getPublicId().equals(f.relevanceId));
        assertAnyTrue(topics[0].filters, f -> supplementary.getPublicId().equals(f.relevanceId));
        assertAnyTrue(topics[0].filters, f -> vg2.getPublicId().equals(f.id));
    }

    @Test
    public void can_get_topics_recursively_with_filter() throws Exception {
        URI subjectid = builder.subject("subject", s -> s
                .name("subject")
                .publicId("urn:subject:1")
                .topic("parent", parent -> parent
                        .name("parent topic")
                        .publicId("urn:topic:a")
                        .subtopic("child", child -> child
                                .name("child topic")
                                .publicId("urn:topic:aa")
                                .filter(vg1, core)
                        )
                )
        ).getPublicId();

        MockHttpServletResponse response = getResource("/v1/subjects/" + subjectid + "/topics?recursive=true&filter=" + vg1.getPublicId());
        Subjects.SubTopicIndexDocument[] topics = getObject(Subjects.SubTopicIndexDocument[].class, response);

        assertEquals(2, topics.length);
        assertAnyTrue(topics, t -> t.name.equals("parent topic"));
        assertAnyTrue(topics, t -> t.name.equals("child topic"));
    }

    @Test
    public void can_get_topic_without_filter_but_underlying_resource_has_filter() throws Exception {
        Subject subject = builder.subject(s -> s
                .name("physics")
                .topic(t -> t.name("statics").resource(r -> r.name("Introduction to statics").filter(vg1, core)))
                .topic(t -> t.name("optics").filter(vg1, core))
        );

        MockHttpServletResponse response = getResource("/v1/subjects/" + subject.getPublicId() + "/topics?filter=" + vg1.getPublicId());
        Subjects.SubTopicIndexDocument[] topics = getObject(Subjects.SubTopicIndexDocument[].class, response);

        assertEquals(2, topics.length);
        assertAnyTrue(topics, t -> "statics".equals(t.name));
        assertAnyTrue(topics, t -> "optics".equals(t.name));
    }

    @Test
    public void can_get_resources_with_relevance() throws Exception {
        URI subjectId = builder.subject(s -> s
                .name("subject")
                .topic(t -> t
                        .name("a")
                        .subtopic(sub -> sub
                                .name("subtopic")
                                .resource(r -> r
                                        .name("a lecture in a subtopic")
                                        .filter(vg1, core)))
                        .resource(r -> r.name("an assignment").filter(vg1, core))
                        .resource(r -> r.name("a lecture").filter(vg1, supplementary))
                )
        ).getPublicId();

        MockHttpServletResponse response = getResource("/v1/subjects/" + subjectId + "/resources?filter=" + vg1.getPublicId() + "&relevance=" + core.getPublicId());
        Topics.ResourceIndexDocument[] result = getObject(Topics.ResourceIndexDocument[].class, response);

        assertEquals(2, result.length);
        assertAnyTrue(result, r -> "a lecture in a subtopic".equals(r.name));
        assertAnyTrue(result, r -> "an assignment".equals(r.name));
    }

    @Test
    public void can_get_topics_by_relevance() throws Exception {
        URI subjectid = builder.subject("subject", s -> s
                .name("subject")
                .publicId("urn:subject:1")
                .topic("parent", parent -> parent
                        .name("parent topic")
                        .publicId("urn:topic:a")
                        .subtopic("child", child -> child
                                .name("child topic")
                                .publicId("urn:topic:aa")
                                .filter(vg1, core)
                        )
                        .subtopic("child2", child -> child
                                .name("child 2")
                                .publicId("urn:topic:ab")
                                .filter(vg1, supplementary))
                )
        ).getPublicId();

        MockHttpServletResponse response = getResource("/v1/subjects/" + subjectid + "/topics?recursive=true&filter=" + vg1.getPublicId() + "&relevance=" + core.getPublicId());
        Subjects.SubTopicIndexDocument[] topics = getObject(Subjects.SubTopicIndexDocument[].class, response);

        assertEquals(2, topics.length);
        assertAnyTrue(topics, t -> t.name.equals("parent topic"));
        assertAnyTrue(topics, t -> t.name.equals("child topic"));
    }
}
