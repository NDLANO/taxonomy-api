package no.ndla.taxonomy.service.rest.v1;


import no.ndla.taxonomy.service.domain.Filter;
import no.ndla.taxonomy.service.domain.Relevance;
import no.ndla.taxonomy.service.domain.Resource;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;

import static no.ndla.taxonomy.service.TestUtils.*;
import static org.junit.Assert.assertEquals;

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

    @Ignore //TODO
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
        Resources.FilterIndexDocument[] filters = getObject(Resources.FilterIndexDocument[].class, response);

        assertEquals(2, filters.length);
    }

}
