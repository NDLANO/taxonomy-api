package no.ndla.taxonomy.rest.v1;

import no.ndla.taxonomy.domain.Filter;
import no.ndla.taxonomy.domain.Relevance;
import no.ndla.taxonomy.domain.Subject;
import no.ndla.taxonomy.rest.v1.dtos.subjects.FilterIndexDocument;
import no.ndla.taxonomy.rest.v1.dtos.subjects.SubTopicIndexDocument;
import no.ndla.taxonomy.rest.v1.dtos.topics.ResourceIndexDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;

import static no.ndla.taxonomy.TestUtils.assertAnyTrue;
import static org.junit.Assert.assertEquals;

public class SubjectFiltersTest extends RestTest {
    private Filter vg1, vg2;
    private Relevance core, supplementary;

    @BeforeEach
    public void before() {
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

        MockHttpServletResponse response = testUtils.getResource("/v1/subjects/urn:subject:1/filters");
        FilterIndexDocument[] filters = testUtils.getObject(FilterIndexDocument[].class, response);

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

        MockHttpServletResponse response = testUtils.getResource("/v1/subjects/" + subjectId + "/resources?filter=" + vg1.getPublicId());
        ResourceIndexDocument[] result = testUtils.getObject(ResourceIndexDocument[].class, response);

        assertEquals(2, result.length);
        assertAnyTrue(result, r -> "a lecture in a subtopic".equals(r.name));
        assertAnyTrue(result, r -> "an assignment".equals(r.name));
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

        MockHttpServletResponse response = testUtils.getResource("/v1/subjects/" + subject.getPublicId() + "/topics?filter=" + vg1.getPublicId());
        SubTopicIndexDocument[] topics = testUtils.getObject(SubTopicIndexDocument[].class, response);

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

        MockHttpServletResponse response = testUtils.getResource("/v1/subjects/" + subject.getPublicId() + "/topics");
        SubTopicIndexDocument[] topics = testUtils.getObject(SubTopicIndexDocument[].class, response);

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

        MockHttpServletResponse response = testUtils.getResource("/v1/subjects/" + subjectid + "/topics?recursive=true&filter=" + vg1.getPublicId());
        SubTopicIndexDocument[] topics = testUtils.getObject(SubTopicIndexDocument[].class, response);

        assertEquals(2, topics.length);
        assertAnyTrue(topics, t -> t.name.equals("parent topic"));
        assertAnyTrue(topics, t -> t.name.equals("child topic"));
    }


    /**
     * taken out while experimenting with changes to the way filters work
     * public void can_get_topic_without_filter_but_underlying_resource_has_filter() throws Exception {
     * Subject subject = builder.subject(s -> s
     * .name("physics")
     * .topic(t -> t.name("statics").resource(r -> r.name("Introduction to statics").filter(vg1, core)))
     * .topic(t -> t.name("optics").filter(vg1, core))
     * );
     * <p>
     * MockHttpServletResponse response = getResource("/v1/subjects/" + subject.getPublicId() + "/topics?filter=" + vg1.getPublicId());
     * Subjects.SubTopicIndexDocument[] topics = getObject(Subjects.SubTopicIndexDocument[].class, response);
     * <p>
     * assertEquals(2, topics.length);
     * assertAnyTrue(topics, t -> "statics".equals(t.name));
     * assertAnyTrue(topics, t -> "optics".equals(t.name));
     * }
     */

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

        MockHttpServletResponse response = testUtils.getResource("/v1/subjects/" + subjectId + "/resources?filter=" + vg1.getPublicId() + "&relevance=" + core.getPublicId());
        ResourceIndexDocument[] result = testUtils.getObject(ResourceIndexDocument[].class, response);

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

        MockHttpServletResponse response = testUtils.getResource("/v1/subjects/" + subjectid + "/topics?recursive=true&filter=" + vg1.getPublicId() + "&relevance=" + core.getPublicId());
        SubTopicIndexDocument[] topics = testUtils.getObject(SubTopicIndexDocument[].class, response);

        assertEquals(2, topics.length);
        assertAnyTrue(topics, t -> t.name.equals("parent topic"));
        assertAnyTrue(topics, t -> t.name.equals("child topic"));
    }
}
