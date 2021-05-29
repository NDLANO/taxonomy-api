package no.ndla.taxonomy.rest.v1;


import no.ndla.taxonomy.domain.Relevance;
import no.ndla.taxonomy.domain.ResourceType;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.rest.v1.dtos.topics.ResourceIndexDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;

import static no.ndla.taxonomy.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TopicFiltersTest extends RestTest {
    private Relevance core, supplementary;

    @BeforeEach
    public void before() {
        core = builder.relevance(r -> r.publicId("urn:relevance:core").name("Core material"));
        supplementary = builder.relevance(r -> r.publicId("urn:relevance:supplementary").name("Supplementary material"));
    }

    @Test
    public void can_list_filters_on_topic() throws Exception {
        builder.topic(t -> t
                .publicId("urn:topic:1")
        );

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:1/filters");
        final var filters = testUtils.getObject(Object[].class, response);

        // Filters are removed
        assertEquals(0, filters.length);
    }

    @Test
    public void can_list_all_topic_filters() throws Exception {
        builder.topic(t -> t
                .publicId("urn:topic:1")
        );

        builder.topic(t -> t
                .publicId("urn:topic:2"));

        MockHttpServletResponse response = testUtils.getResource("/v1/topic-filters");
        TopicFilters.TopicFilterIndexDocument[] topicFilters = testUtils.getObject(TopicFilters.TopicFilterIndexDocument[].class, response);
        assertEquals(0, topicFilters.length);
    }

    @Test
    public void can_get_resources_belonging_to_a_filter_for_a_topic() throws Exception {
        builder.topic(t -> t
                .publicId("urn:topic:1")
                .resource(r -> r
                        .publicId("urn:resource:1")
                )
                .resource(r -> r
                        .publicId("urn:resource:2")
                )
        );

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:1/resources?filter=urn:filter:1:1");
        ResourceIndexDocument[] result = testUtils.getObject(ResourceIndexDocument[].class, response);

        assertEquals(0, result.length);
    }

    @Test
    public void can_get_resources_for_a_topic_belonging_to_a_filter_recursively() throws Exception {
        builder.subject(s -> s
                .name("subject a")
                .topic(t -> t
                        .name("a")
                        .publicId("urn:topic:a")
                        .resource(r -> r.name("resource a").contentUri("urn:article:a"))
                        .subtopic(st -> st
                                .name("aa")
                                .resource(r -> r.name("resource aa").contentUri("urn:article:aa"))
                                .subtopic(st2 -> st2
                                        .name("aaa")
                                        .resource(r -> r.name("resource aaa").contentUri("urn:article:aaa"))
                                )
                                .subtopic(st2 -> st2
                                        .name("aab")
                                        .resource(r -> r.name("resource aab").contentUri("urn:article:aab"))
                                )
                        )
                ));

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:a/resources?recursive=true&filter=urn:filter:1:1");
        ResourceIndexDocument[] result = testUtils.getObject(ResourceIndexDocument[].class, response);

        assertEquals(0, result.length);
    }

    @Test
    public void can_get_resources_for_a_topic_with_filter_and_resource_type() throws Exception {
        ResourceType type = builder.resourceType(rt -> rt.name("Subject matter").publicId("urn:resourcetype:subject-matter"));

        builder.topic(t -> t
                .publicId("urn:topic:1")
                .resource(r -> r
                        .publicId("urn:resource:1")
                        .resourceType(type)
                )
                .resource(r -> r
                        .publicId("urn:resource:2")
                )
        );

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:1/resources?type=" + type.getPublicId() + "&filter=urn:filter:1:1");
        ResourceIndexDocument[] result = testUtils.getObject(ResourceIndexDocument[].class, response);

        // Filters are removed
        assertEquals(0, result.length);
    }

    @Test
    public void can_get_resources_for_a_topic_with_resource_type() throws Exception {
        ResourceType type = builder.resourceType(rt -> rt.name("Subject matter").publicId("urn:resourcetype:subject-matter"));

        builder.topic(t -> t
                .publicId("urn:topic:1")
                .resource(r -> r
                        .publicId("urn:resource:1")
                        .resourceType(type)
                )
                .resource(r -> r
                        .publicId("urn:resource:2")
                )
        );

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:1/resources?type=" + type.getPublicId());
        ResourceIndexDocument[] result = testUtils.getObject(ResourceIndexDocument[].class, response);

        assertEquals(1, result.length);
        assertEquals("urn:resource:1", result[0].id.toString());
    }

    @Test
    public void can_get_recursive_resources_with_filter_and_resource_type_restrictions_for_a_topic() throws Exception {
        ResourceType type = builder.resourceType(rt -> rt.name("Subject matter").publicId("urn:resourcetype:subject-matter"));

        builder.subject(s -> s
                .name("subject a")
                .topic(t -> t
                        .name("a")
                        .publicId("urn:topic:a")
                        .resource(r -> r.name("resource a").contentUri("urn:article:a").resourceType(type))
                        .subtopic(st -> st
                                .name("aa")
                                .resource(r -> r.name("resource aa").contentUri("urn:article:aa").resourceType(type))
                                .subtopic(st2 -> st2
                                        .name("aaa")
                                        .resource(r -> r.name("resource aaa").contentUri("urn:article:aaa"))
                                )
                                .subtopic(st2 -> st2
                                        .name("aab")
                                        .resource(r -> r.name("resource aab").contentUri("urn:article:aab"))
                                )
                        )
                ));

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:a/resources?recursive=true&filter=urn:filter:1:1&type=" + type.getPublicId());
        ResourceIndexDocument[] result = testUtils.getObject(ResourceIndexDocument[].class, response);

        assertEquals(0, result.length);
    }

    //@Test - TODO - should test new relevance field
    public void can_get_resources_by_relevance() throws Exception {
        builder.topic(t -> t
                .publicId("urn:topic:1")
                .resource(r -> r
                        .publicId("urn:resource:1")
                        /*.filter(vg1, core)*/
                )
                .resource(r -> r
                        .publicId("urn:resource:2")
                        /*.filter(vg1, supplementary)*/
                )
        );

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:1/resources?relevance=" + core.getPublicId());
        ResourceIndexDocument[] result = testUtils.getObject(ResourceIndexDocument[].class, response);

        assertEquals(1, result.length);
        assertEquals("urn:resource:1", result[0].id.toString());
    }

    //@Test TODO - should test new relevance field
    public void can_get_resources_by_relevance_recursively() throws Exception {
        builder.subject(s -> s
                .name("subject a")
                .topic(t -> t
                        .name("a")
                        .publicId("urn:topic:a")
                        .resource(r -> r.name("resource a").contentUri("urn:article:a")/*.filter(vg1, core)*/)
                        .subtopic(st -> st
                                .name("aa")
                                .resource(r -> r.name("resource aa").contentUri("urn:article:aa")/*.filter(vg1, core)*/)
                                .subtopic(st2 -> st2
                                        .name("aaa")
                                        .resource(r -> r.name("resource aaa").contentUri("urn:article:aaa")/*.filter(vg1, core)*/)
                                )
                                .subtopic(st2 -> st2
                                        .name("aab")
                                        .resource(r -> r.name("resource aab").contentUri("urn:article:aab")/*.filter(vg1, supplementary)*/)
                                )
                        )
                ));

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:a/resources?recursive=true&relevance=" + core.getPublicId());
        ResourceIndexDocument[] result = testUtils.getObject(ResourceIndexDocument[].class, response);

        assertEquals(3, result.length);
        assertAnyTrue(result, r -> "resource a".equals(r.name) && "urn:article:a".equals(r.contentUri.toString()));
        assertAnyTrue(result, r -> "resource aa".equals(r.name) && "urn:article:aa".equals(r.contentUri.toString()));
        assertAnyTrue(result, r -> "resource aaa".equals(r.name) && "urn:article:aaa".equals(r.contentUri.toString()));
    }
}
