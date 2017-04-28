package no.ndla.taxonomy.service.rest.v1;


import no.ndla.taxonomy.service.domain.Filter;
import no.ndla.taxonomy.service.domain.Relevance;
import no.ndla.taxonomy.service.domain.ResourceType;
import no.ndla.taxonomy.service.domain.Topic;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;

import static no.ndla.taxonomy.service.TestUtils.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TopicFiltersTest extends RestTest {

    @Test
    public void can_add_filter_to_topic() throws Exception {
        Topic topic = builder.topic(t -> t.publicId("urn:topic:1"));
        builder.filter(f -> f.publicId("urn:filter:1"));
        builder.relevance(r -> r.publicId("urn:relevance:core").name("Core material"));

        URI id = getId(
                createResource("/v1/topic-filters", new TopicFilters.AddFilterToTopicCommand() {{
                    topicId = URI.create("urn:topic:1");
                    filterId = URI.create("urn:filter:1");
                    relevanceId = URI.create("urn:relevance:core");
                }})
        );

        assertEquals(1, topic.filters.size());
        assertEquals(first(topic.filters).getPublicId(), id);
        assertEquals("urn:relevance:core", first(topic.filters).getRelevance().getPublicId().toString());
    }

    @Test
    public void can_list_filters_on_topic() throws Exception {
        Filter filter1 = builder.filter(f -> f.publicId("urn:filter:1"));
        Filter filter2 = builder.filter(f -> f.publicId("urn:filter:2"));
        Relevance relevance = builder.relevance(r -> r.publicId("urn:relevance:core").name("Core material"));

        builder.topic(t -> t
                .publicId("urn:topic:1")
                .filter(filter1, relevance)
                .filter(filter2, relevance)
        );

        MockHttpServletResponse response = getResource("/v1/topics/urn:topic:1/filters");
        Topics.FilterIndexDocument[] filters = getObject(Topics.FilterIndexDocument[].class, response);

        assertEquals(2, filters.length);
        assertAnyTrue(filters, f -> f.id.equals(filter1.getPublicId()));
        assertAnyTrue(filters, f -> f.id.equals(filter2.getPublicId()));
        assertAllTrue(filters, f -> f.relevanceId.equals(relevance.getPublicId()));
    }

    @Test
    public void cannot_have_duplicate_filters_for_topic() throws Exception {
        Filter filter = builder.filter(f -> f.publicId("urn:filter:1"));
        Relevance relevance = builder.relevance(r -> r.publicId("urn:relevance:core").name("Core material"));

        builder.topic(t -> t
                .publicId("urn:topic:1")
                .filter(filter, relevance)
        );

        createResource("/v1/topic-filters", new TopicFilters.AddFilterToTopicCommand() {{
            topicId = URI.create("urn:topic:1");
            filterId = URI.create("urn:filter:1");
            relevanceId = URI.create("urn:relevance:core");
        }}, status().isConflict());
    }

    @Test
    public void can_remove_filter_from_topic() throws Exception {
        Filter filter = builder.filter(f -> f.publicId("urn:filter:2"));
        Relevance relevance = builder.relevance(r -> r.publicId("urn:relevance:core").name("Core material"));

        Topic topic = builder.topic(t -> t
                .publicId("urn:topic:1"));

        URI id = save(topic.addFilter(filter, relevance)).getPublicId();

        deleteResource("/v1/topic-filters/" + id);
        assertNull(topicFilterRepository.findByPublicId(id));
    }

    @Test
    public void can_change_relevance_for_filter() throws Exception {
        Filter filter = builder.filter(f -> f.publicId("urn:filter:2"));
        Relevance core = builder.relevance(r -> r.publicId("urn:relevance:core").name("Core material"));
        Relevance supplementary = builder.relevance(r -> r.publicId("urn:relevance:supplementary").name("Supplementary material"));

        Topic topic = builder.topic(t -> t
                .publicId("urn:topic:1"));

        URI id = save(topic.addFilter(filter, core)).getPublicId();

        updateResource("/v1/topic-filters/" + id, new TopicFilters.UpdateTopicFilterCommand() {{
            relevanceId = supplementary.getPublicId();
        }});
        assertEquals("urn:relevance:supplementary", first(topic.filters).getRelevance().getPublicId().toString());
    }

    @Test
    public void can_list_all_topic_filters() throws Exception {
        Filter filter = builder.filter(f -> f.publicId("urn:filter:2"));
        Relevance relevance = builder.relevance(r -> r.publicId("urn:relevance:core").name("Core material"));
        builder.topic(t -> t
                .publicId("urn:topic:1")
                .filter(filter, relevance)
        );

        builder.topic(t -> t
        .publicId("urn:topic:2")
        .filter(filter, relevance));

        MockHttpServletResponse response = getResource("/v1/topic-filters");
        TopicFilters.TopicFilterIndexDocument[] topicFilters = getObject(TopicFilters.TopicFilterIndexDocument[].class, response);
        assertEquals(2, topicFilters.length);
        assertAnyTrue(topicFilters, rf -> URI.create("urn:topic:1").equals(rf.topicId) && filter.getPublicId().equals(rf.filterId) && relevance.getPublicId().equals(rf.relevanceId));
        assertAnyTrue(topicFilters, rf -> URI.create("urn:topic:2").equals(rf.topicId) && filter.getPublicId().equals(rf.filterId) && relevance.getPublicId().equals(rf.relevanceId));
    }


    @Test
    public void can_get_resources_belonging_to_a_filter_for_a_topic() throws Exception {
        Filter vg1 = builder.filter(f -> f.publicId("urn:filter:vg1"));
        Relevance core = builder.relevance(r -> r.publicId("urn:relevance:core"));

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

        MockHttpServletResponse response = getResource("/v1/topics/urn:topic:1/resources?filter=" + vg1.getPublicId());
        Topics.ResourceIndexDocument[] result = getObject(Topics.ResourceIndexDocument[].class, response);

        assertEquals(1, result.length);
        assertEquals("urn:resource:1", result[0].id.toString());
    }

    @Test
    public void can_get_resources_for_a_topic_belonging_to_a_filter_recursively() throws Exception {
        Filter vg1 = builder.filter(f -> f.publicId("urn:filter:vg1"));
        Relevance core = builder.relevance(r -> r.publicId("urn:relevance:core"));
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

        MockHttpServletResponse response = getResource("/v1/topics/urn:topic:a/resources?recursive=true&filter=" + vg1.getPublicId());
        Topics.ResourceIndexDocument[] result = getObject(Topics.ResourceIndexDocument[].class, response);

        assertEquals(3, result.length);
        assertAnyTrue(result, r -> "resource a".equals(r.name) && "urn:article:a".equals(r.contentUri.toString()));
        assertAnyTrue(result, r -> "resource aa".equals(r.name) && "urn:article:aa".equals(r.contentUri.toString()));
        assertAnyTrue(result, r -> "resource aaa".equals(r.name) && "urn:article:aaa".equals(r.contentUri.toString()));
    }

    @Test
    public void can_get_resources_for_a_topic_with_filter_and_resource_type() throws Exception {
        Filter vg1 = builder.filter(f -> f.publicId("urn:filter:vg1"));
        Relevance core = builder.relevance(r -> r.publicId("urn:relevance:core"));
        ResourceType type = builder.resourceType(rt -> rt.name("Subject matter").publicId("urn:resource-type:subject-matter"));

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

        MockHttpServletResponse response = getResource("/v1/topics/urn:topic:1/resources?type=" + type.getPublicId() + "&filter=" + vg1.getPublicId());
        Topics.ResourceIndexDocument[] result = getObject(Topics.ResourceIndexDocument[].class, response);

        assertEquals(1, result.length);
        assertEquals("urn:resource:1", result[0].id.toString());
    }

    @Test
    public void can_get_recursive_resources_with_filter_and_resource_type_restrictions_for_a_topic() throws Exception {
        Filter vg1 = builder.filter(f -> f.publicId("urn:filter:vg1"));
        Relevance core = builder.relevance(r -> r.publicId("urn:relevance:core"));
        ResourceType type = builder.resourceType(rt -> rt.name("Subject matter").publicId("urn:resource-type:subject-matter"));

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

        MockHttpServletResponse response = getResource("/v1/topics/urn:topic:a/resources?recursive=true&filter=" + vg1.getPublicId() + "&type=" + type.getPublicId());
        Topics.ResourceIndexDocument[] result = getObject(Topics.ResourceIndexDocument[].class, response);

        assertEquals(2, result.length);
        assertAnyTrue(result, r -> "resource a".equals(r.name) && "urn:article:a".equals(r.contentUri.toString()));
        assertAnyTrue(result, r -> "resource aa".equals(r.name) && "urn:article:aa".equals(r.contentUri.toString()));
    }
}
