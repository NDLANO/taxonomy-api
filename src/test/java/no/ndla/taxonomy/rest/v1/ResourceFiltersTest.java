package no.ndla.taxonomy.rest.v1;


import no.ndla.taxonomy.domain.Filter;
import no.ndla.taxonomy.domain.Relevance;
import no.ndla.taxonomy.domain.Resource;
import no.ndla.taxonomy.rest.v1.dtos.resources.FilterIndexDocument;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;

import static no.ndla.taxonomy.TestUtils.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ResourceFiltersTest extends RestTest {

    @Test
    public void can_add_filter_to_resource() throws Exception {
        Resource resource = builder.resource(r -> r.publicId("urn:resource:1"));
        builder.filter(f -> f.publicId("urn:filter:1"));
        builder.relevance(r -> r.publicId("urn:relevance:core").name("Core material"));

        URI id = getId(
                createResource("/v1/resource-filters", new ResourceFilters.AddFilterToResourceCommand() {{
                    resourceId = URI.create("urn:resource:1");
                    filterId = URI.create("urn:filter:1");
                    relevanceId = URI.create("urn:relevance:core");
                }})
        );

        assertEquals(1, resource.filters.size());
        assertEquals(first(resource.filters).getPublicId(), id);
        assertEquals("urn:relevance:core", first(resource.filters).getRelevance().getPublicId().toString());
    }

    @Test
    public void can_list_filters_on_resource() throws Exception {
        Filter filter1 = builder.filter(f -> f.publicId("urn:filter:1"));
        Filter filter2 = builder.filter(f -> f.publicId("urn:filter:2"));
        Relevance relevance = builder.relevance(r -> r.publicId("urn:relevance:core").name("Core material"));

        builder.resource(r -> r
                .publicId("urn:resource:1")
                .filter(filter1, relevance)
                .filter(filter2, relevance)
        );

        MockHttpServletResponse response = getResource("/v1/resources/urn:resource:1/filters");
        FilterIndexDocument[] filters = getObject(FilterIndexDocument[].class, response);

        assertEquals(2, filters.length);
        assertAnyTrue(filters, f -> f.id.equals(filter1.getPublicId()));
        assertAnyTrue(filters, f -> f.id.equals(filter2.getPublicId()));
        assertAllTrue(filters, f -> f.relevanceId.equals(relevance.getPublicId()));
    }

    @Test
    public void cannot_have_duplicate_filters_for_resource() throws Exception {
        Filter filter = builder.filter(f -> f.publicId("urn:filter:1"));
        Relevance relevance = builder.relevance(r -> r.publicId("urn:relevance:core").name("Core material"));

        builder.resource(r -> r
                .publicId("urn:resource:1")
                .filter(filter, relevance)
        );

        createResource("/v1/resource-filters", new ResourceFilters.AddFilterToResourceCommand() {{
            resourceId = URI.create("urn:resource:1");
            filterId = URI.create("urn:filter:1");
            relevanceId = URI.create("urn:relevance:core");
        }}, status().isConflict());
    }

    @Test
    public void can_remove_filter_from_resource() throws Exception {
        Filter filter = builder.filter(f -> f.publicId("urn:filter:2"));
        Relevance relevance = builder.relevance(r -> r.publicId("urn:relevance:core").name("Core material"));

        Resource resource = builder.resource(r -> r
                .publicId("urn:resource:1"));

        URI id = save(resource.addFilter(filter, relevance)).getPublicId();

        deleteResource("/v1/resource-filters/" + id);
        assertNull(resourceFilterRepository.findByPublicId(id));
    }

    @Test
    public void can_change_relevance_for_filter() throws Exception {
        Filter filter = builder.filter(f -> f.publicId("urn:filter:2"));
        Relevance core = builder.relevance(r -> r.publicId("urn:relevance:core").name("Core material"));
        Relevance supplementary = builder.relevance(r -> r.publicId("urn:relevance:supplementary").name("Supplementary material"));


        Resource resource = builder.resource(r -> r
                .publicId("urn:resource:1"));

        URI id = save(resource.addFilter(filter, core)).getPublicId();

        updateResource("/v1/resource-filters/" + id, new ResourceFilters.UpdateResourceFilterCommand() {{
            relevanceId = supplementary.getPublicId();
        }});
        assertEquals("urn:relevance:supplementary", first(resource.filters).getRelevance().getPublicId().toString());
    }

    @Test
    public void can_list_all_resource_filters() throws Exception {
        Filter filter = builder.filter(f -> f.publicId("urn:filter:2"));
        Relevance relevance = builder.relevance(r -> r.publicId("urn:relevance:core").name("Core material"));
        builder.resource(r -> r
                .publicId("urn:resource:1")
                .filter(filter, relevance)
        );

        builder.resource(r -> r
                .publicId("urn:resource:2")
                .filter(filter, relevance));

        MockHttpServletResponse response = getResource("/v1/resource-filters");
        ResourceFilters.ResourceFilterIndexDocument[] resourceFilters = getObject(ResourceFilters.ResourceFilterIndexDocument[].class, response);
        assertEquals(2, resourceFilters.length);
        assertAnyTrue(resourceFilters, rf -> URI.create("urn:resource:1").equals(rf.resourceId) && filter.getPublicId().equals(rf.filterId) && relevance.getPublicId().equals(rf.relevanceId));
        assertAnyTrue(resourceFilters, rf -> URI.create("urn:resource:2").equals(rf.resourceId) && filter.getPublicId().equals(rf.filterId) && relevance.getPublicId().equals(rf.relevanceId));
    }
}
