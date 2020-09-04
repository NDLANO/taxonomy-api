package no.ndla.taxonomy.rest.v1;


import no.ndla.taxonomy.domain.Filter;
import no.ndla.taxonomy.domain.Relevance;
import no.ndla.taxonomy.domain.ResourceType;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.rest.v1.dtos.topics.ResourceIndexDocument;
import no.ndla.taxonomy.service.dtos.FilterWithConnectionDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;

import static no.ndla.taxonomy.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TopicFiltersTest extends RestTest {
    private Filter vg1, vg2;
    private Relevance core, supplementary;

    @BeforeEach
    public void before() {
        core = builder.relevance(r -> r.publicId("urn:relevance:core").name("Core material"));
        vg1 = builder.filter(f -> f.publicId("urn:filter:vg1"));
        vg2 = builder.filter(f -> f.publicId("urn:filter:vg2"));
        supplementary = builder.relevance(r -> r.publicId("urn:relevance:supplementary").name("Supplementary material"));
    }

    @Test
    public void can_add_filter_to_topic() throws Exception {
        Topic topic = builder.topic(t -> t.publicId("urn:topic:1"));

        URI id = getId(
                testUtils.createResource("/v1/topic-filters", new TopicFilters.AddFilterToTopicCommand() {{
                    topicId = URI.create("urn:topic:1");
                    filterId = URI.create("urn:filter:vg1");
                    relevanceId = URI.create("urn:relevance:core");
                }})
        );

        assertEquals(1, topic.getTopicFilters().size());
        assertEquals(first(topic.getTopicFilters()).getPublicId(), id);
        assertEquals("urn:relevance:core", first(topic.getTopicFilters()).getRelevance().get().getPublicId().toString());
    }

    @Test
    public void can_list_filters_on_topic() throws Exception {
        builder.topic(t -> t
                .publicId("urn:topic:1")
                .filter(vg1, core)
                .filter(vg2, core)
        );

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:1/filters");
        final var filters = testUtils.getObject(FilterWithConnectionDTO[].class, response);

        assertEquals(2, filters.length);
        assertAnyTrue(filters, f -> f.getId().orElseThrow().equals(vg1.getPublicId()));
        assertAnyTrue(filters, f -> f.getId().orElseThrow().equals(vg2.getPublicId()));
        assertAllTrue(filters, f -> f.getRelevanceId().equals(core.getPublicId()));
    }

    @Test
    public void cannot_have_duplicate_filters_for_topic() throws Exception {
        builder.topic(t -> t
                .publicId("urn:topic:1")
                .filter(vg1, core)
        );

        testUtils.createResource("/v1/topic-filters", new TopicFilters.AddFilterToTopicCommand() {{
            topicId = URI.create("urn:topic:1");
            filterId = URI.create("urn:filter:vg1");
            relevanceId = URI.create("urn:relevance:core");
        }}, status().isConflict());
    }

    @Test
    public void can_remove_filter_from_topic() throws Exception {
        URI id;
        {
            Topic topic = builder.topic(t -> t
                    .publicId("urn:topic:1"));

            id = save(topic.addFilter(vg1, core)).getPublicId();
        }

        assertNotNull(topicFilterRepository.findByPublicId(id));
        testUtils.deleteResource("/v1/topic-filters/" + id);
        assertNull(topicFilterRepository.findByPublicId(id));
    }

    @Test
    public void can_change_relevance_for_filter() throws Exception {
        Topic topic = builder.topic(t -> t.publicId("urn:topic:1"));
        URI id = save(topic.addFilter(vg1, core)).getPublicId();

        testUtils.updateResource("/v1/topic-filters/" + id, new TopicFilters.UpdateTopicFilterCommand() {{
            relevanceId = supplementary.getPublicId();
        }});

        assertEquals("urn:relevance:supplementary", first(topic.getTopicFilters()).getRelevance().get().getPublicId().toString());
    }

    @Test
    public void can_list_all_topic_filters() throws Exception {
        builder.topic(t -> t
                .publicId("urn:topic:1")
                .filter(vg1, core)
        );

        builder.topic(t -> t
                .publicId("urn:topic:2")
                .filter(vg1, core));

        MockHttpServletResponse response = testUtils.getResource("/v1/topic-filters");
        TopicFilters.TopicFilterIndexDocument[] topicFilters = testUtils.getObject(TopicFilters.TopicFilterIndexDocument[].class, response);
        assertEquals(2, topicFilters.length);
        assertAnyTrue(topicFilters, rf -> URI.create("urn:topic:1").equals(rf.topicId) && vg1.getPublicId().equals(rf.filterId) && core.getPublicId().equals(rf.relevanceId));
        assertAnyTrue(topicFilters, rf -> URI.create("urn:topic:2").equals(rf.topicId) && vg1.getPublicId().equals(rf.filterId) && core.getPublicId().equals(rf.relevanceId));
    }

    @Test
    public void can_get_resources_belonging_to_a_filter_for_a_topic() throws Exception {
        builder.topic(t -> t
                .publicId("urn:topic:1")
                .resource(r -> r
                        .publicId("urn:resource:1")
                        .filter(vg1, core)
                )
                .resource(r -> r
                        .publicId("urn:resource:2")
                )
        );

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:1/resources?filter=" + vg1.getPublicId());
        ResourceIndexDocument[] result = testUtils.getObject(ResourceIndexDocument[].class, response);

        assertEquals(1, result.length);
        assertEquals("urn:resource:1", result[0].id.toString());
    }

    @Test
    public void can_get_resources_for_a_topic_belonging_to_a_filter_recursively() throws Exception {
        builder.subject(s -> s
                .name("subject a")
                .topic(t -> t
                        .name("a")
                        .publicId("urn:topic:a")
                        .resource(r -> r.name("resource a").contentUri("urn:article:a").filter(vg1, core))
                        .subtopic(st -> st
                                .name("aa")
                                .resource(r -> r.name("resource aa").contentUri("urn:article:aa").filter(vg1, core))
                                .subtopic(st2 -> st2
                                        .name("aaa")
                                        .resource(r -> r.name("resource aaa").contentUri("urn:article:aaa").filter(vg1, core))
                                )
                                .subtopic(st2 -> st2
                                        .name("aab")
                                        .resource(r -> r.name("resource aab").contentUri("urn:article:aab"))
                                )
                        )
                ));

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:a/resources?recursive=true&filter=" + vg1.getPublicId());
        ResourceIndexDocument[] result = testUtils.getObject(ResourceIndexDocument[].class, response);

        assertEquals(3, result.length);
        assertAnyTrue(result, r -> "resource a".equals(r.name) && "urn:article:a".equals(r.contentUri.toString()));
        assertAnyTrue(result, r -> "resource aa".equals(r.name) && "urn:article:aa".equals(r.contentUri.toString()));
        assertAnyTrue(result, r -> "resource aaa".equals(r.name) && "urn:article:aaa".equals(r.contentUri.toString()));
    }

    @Test
    public void can_get_resources_for_a_topic_with_filter_and_resource_type() throws Exception {
        ResourceType type = builder.resourceType(rt -> rt.name("Subject matter").publicId("urn:resourcetype:subject-matter"));

        builder.topic(t -> t
                .publicId("urn:topic:1")
                .resource(r -> r
                        .publicId("urn:resource:1")
                        .filter(vg1, core)
                        .resourceType(type)
                )
                .resource(r -> r
                        .publicId("urn:resource:2")
                )
        );

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:1/resources?type=" + type.getPublicId() + "&filter=" + vg1.getPublicId());
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
                        .resource(r -> r.name("resource a").contentUri("urn:article:a").filter(vg1, core).resourceType(type))
                        .subtopic(st -> st
                                .name("aa")
                                .resource(r -> r.name("resource aa").contentUri("urn:article:aa").filter(vg1, core).resourceType(type))
                                .subtopic(st2 -> st2
                                        .name("aaa")
                                        .resource(r -> r.name("resource aaa").contentUri("urn:article:aaa").filter(vg1, core))
                                )
                                .subtopic(st2 -> st2
                                        .name("aab")
                                        .resource(r -> r.name("resource aab").contentUri("urn:article:aab"))
                                )
                        )
                ));

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:a/resources?recursive=true&filter=" + vg1.getPublicId() + "&type=" + type.getPublicId());
        ResourceIndexDocument[] result = testUtils.getObject(ResourceIndexDocument[].class, response);

        assertEquals(2, result.length);
        assertAnyTrue(result, r -> "resource a".equals(r.name) && "urn:article:a".equals(r.contentUri.toString()));
        assertAnyTrue(result, r -> "resource aa".equals(r.name) && "urn:article:aa".equals(r.contentUri.toString()));
    }

    @Test
    public void can_get_resources_by_relevance() throws Exception {
        builder.topic(t -> t
                .publicId("urn:topic:1")
                .resource(r -> r
                        .publicId("urn:resource:1")
                        .filter(vg1, core)
                )
                .resource(r -> r
                        .publicId("urn:resource:2")
                        .filter(vg1, supplementary)
                )
        );

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:1/resources?filter=" + vg1.getPublicId() + "&relevance=" + core.getPublicId());
        ResourceIndexDocument[] result = testUtils.getObject(ResourceIndexDocument[].class, response);

        assertEquals(1, result.length);
        assertEquals("urn:resource:1", result[0].id.toString());
    }

    @Test
    public void can_get_resources_by_relevance_recursively() throws Exception {
        builder.subject(s -> s
                .name("subject a")
                .topic(t -> t
                        .name("a")
                        .publicId("urn:topic:a")
                        .resource(r -> r.name("resource a").contentUri("urn:article:a").filter(vg1, core))
                        .subtopic(st -> st
                                .name("aa")
                                .resource(r -> r.name("resource aa").contentUri("urn:article:aa").filter(vg1, core))
                                .subtopic(st2 -> st2
                                        .name("aaa")
                                        .resource(r -> r.name("resource aaa").contentUri("urn:article:aaa").filter(vg1, core))
                                )
                                .subtopic(st2 -> st2
                                        .name("aab")
                                        .resource(r -> r.name("resource aab").contentUri("urn:article:aab").filter(vg1, supplementary))
                                )
                        )
                ));

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:a/resources?recursive=true&filter=" + vg1.getPublicId() + "&relevance=" + core.getPublicId());
        ResourceIndexDocument[] result = testUtils.getObject(ResourceIndexDocument[].class, response);

        assertEquals(3, result.length);
        assertAnyTrue(result, r -> "resource a".equals(r.name) && "urn:article:a".equals(r.contentUri.toString()));
        assertAnyTrue(result, r -> "resource aa".equals(r.name) && "urn:article:aa".equals(r.contentUri.toString()));
        assertAnyTrue(result, r -> "resource aaa".equals(r.name) && "urn:article:aaa".equals(r.contentUri.toString()));
    }
}
