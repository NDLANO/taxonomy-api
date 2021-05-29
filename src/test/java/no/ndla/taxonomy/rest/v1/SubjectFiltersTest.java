package no.ndla.taxonomy.rest.v1;

import no.ndla.taxonomy.domain.Relevance;
import no.ndla.taxonomy.domain.Subject;
import no.ndla.taxonomy.rest.v1.dtos.subjects.SubTopicIndexDocument;
import no.ndla.taxonomy.rest.v1.dtos.topics.ResourceIndexDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;

import static no.ndla.taxonomy.TestUtils.assertAnyTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SubjectFiltersTest extends RestTest {
    private Relevance core, supplementary;

    @BeforeEach
    void clearAllRepos() {
        resourceRepository.deleteAllAndFlush();
        topicRepository.deleteAllAndFlush();
        subjectRepository.deleteAllAndFlush();
    }

    @BeforeEach
    public void before() {
        core = builder.relevance(r -> r.publicId("urn:relevance:core").name("Core material"));
        supplementary = builder.relevance(r -> r.publicId("urn:relevance:supplementary").name("Supplementary material"));
    }

    @Test
    public void can_get_filters_for_subject() throws Exception {
        builder.subject(s -> s
                .name("Byggfag")
                .publicId("urn:subject:1")
        );

        final var response = testUtils.getResource("/v1/subjects/urn:subject:1/filters");
        final var filters = testUtils.getObject(Object[].class, response);

        // Filters are removed
        assertEquals(0, filters.length);
    }

    @Test
    public void can_get_resources_belonging_to_a_filter_for_a_subject() throws Exception {
        URI subjectId = builder.subject(s -> s
                .name("subject")
                .topic(t -> t
                        .name("a")
                        .subtopic(sub -> sub.name("subtopic").resource(r -> r.name("a lecture in a subtopic")))
                        .resource(r -> r.name("an assignment"))
                        .resource(r -> r.name("a lecture"))
                )
        ).getPublicId();

        MockHttpServletResponse response = testUtils.getResource("/v1/subjects/" + subjectId + "/resources?filter=urn:filter:1:1");
        ResourceIndexDocument[] result = testUtils.getObject(ResourceIndexDocument[].class, response);

        // Filters are removed
        assertEquals(0, result.length);
    }


    @Test
    public void can_get_topics_with_filter() throws Exception {
        Subject subject = builder.subject(s -> s
                .name("physics")
                .topic(t -> t
                        .name("statics")
                )
                .topic(t -> t.name("electricity"))
                .topic(t -> t.name("optics"))
        );

        MockHttpServletResponse response = testUtils.getResource("/v1/subjects/" + subject.getPublicId() + "/topics?filter=urn:filter:1:1");
        SubTopicIndexDocument[] topics = testUtils.getObject(SubTopicIndexDocument[].class, response);

        // Filters are removed
        assertEquals(0, topics.length);
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
                        )
                )
        ).getPublicId();

        MockHttpServletResponse response = testUtils.getResource("/v1/subjects/" + subjectid + "/topics?recursive=true&filter=urn:filter:1:1");
        SubTopicIndexDocument[] topics = testUtils.getObject(SubTopicIndexDocument[].class, response);

        // Filters are removed
        assertEquals(0, topics.length);
    }


    //@Test TODO - Relevance on connections should be tested
    public void can_get_resources_with_relevance() throws Exception {
        URI subjectId = builder.subject(s -> s
                .name("subject")
                .topic(t -> t
                        .name("a")
                        .subtopic(sub -> sub
                                .name("subtopic")
                                .resource(r -> r
                                        .name("a lecture in a subtopic")
                                        /*.filter(vg1, core)*/))
                        .resource(r -> r.name("an assignment")/*.filter(vg1, core)*/)
                        .resource(r -> r.name("a lecture")/*.filter(vg1, supplementary)*/)
                )
        ).getPublicId();

        MockHttpServletResponse response = testUtils.getResource("/v1/subjects/" + subjectId + "/resources?relevance=" + core.getPublicId());
        ResourceIndexDocument[] result = testUtils.getObject(ResourceIndexDocument[].class, response);

        assertEquals(2, result.length);
        assertAnyTrue(result, r -> "a lecture in a subtopic".equals(r.name));
        assertAnyTrue(result, r -> "an assignment".equals(r.name));
    }

    //@Test TODO - Relevance on connections should be tested
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
                                /*.filter(vg1, core)*/
                        )
                        .subtopic("child2", child -> child
                                .name("child 2")
                                .publicId("urn:topic:ab")
                                /*.filter(vg1, supplementary)*/)
                )
        ).getPublicId();

        MockHttpServletResponse response = testUtils.getResource("/v1/subjects/" + subjectid + "/topics?recursive=true&relevance=" + core.getPublicId());
        SubTopicIndexDocument[] topics = testUtils.getObject(SubTopicIndexDocument[].class, response);

        assertEquals(2, topics.length);
        assertAnyTrue(topics, t -> t.name.equals("parent topic"));
        assertAnyTrue(topics, t -> t.name.equals("child topic"));
    }
}
