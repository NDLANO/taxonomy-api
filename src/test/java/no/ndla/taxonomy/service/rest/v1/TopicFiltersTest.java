package no.ndla.taxonomy.service.rest.v1;


import no.ndla.taxonomy.service.domain.Filter;
import no.ndla.taxonomy.service.domain.Relevance;
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
}
